package com.bepiscorp.ktools.commands.md2pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/** Bridge to the Docker-based md2pdf service. */
class DockerBridge {

    private val log = KotlinLogging.logger {}

    private val imageName = "realdennis/md2pdf:latest"
    private val containerPrefix = "ktools-md2pdf"
    private var reuseContainer = false
    private var currentContainer: String? = null

    /** Convert a markdown input to PDF using Docker md2pdf. */
    suspend fun convert(input: InputFile, output: File, options: Map<String, Any>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Ensure Docker image is available
                ensureDockerImage()

                // Start container if needed
                val containerId = getOrStartContainer(options)

                // Prepare input content
                val inputContent = when (input) {
                    is InputFile.Stdin -> readStdin()
                    is InputFile.Local -> input.file.readText()
                    is InputFile.Remote -> downloadContent(input.url)
                }

                // Create temporary files for Docker transfer
                val tempDir = Files.createTempDirectory("ktools-md2pdf").toFile()
                val inputFile = File(tempDir, "input.md")
                val outputFile = File(tempDir, "output.pdf")

                try {
                    inputFile.writeText(inputContent)

                    // Execute conversion via Docker
                    val success = executeDockerConversion(containerId, inputFile, outputFile, options)

                    if (success && outputFile.exists()) {
                        if (output.path == "-") {
                            // Write to stdout
                            System.out.write(outputFile.readBytes())
                        } else {
                            // Copy to target file
                            output.parentFile?.mkdirs()
                            outputFile.copyTo(output, overwrite = true)
                        }
                        return@withContext true
                    }

                    return@withContext false
                } finally {
                    // Cleanup temporary files
                    tempDir.deleteRecursively()

                    // Stop container if not reusing
                    if (!reuseContainer) {
                        stopContainer(containerId)
                    }
                }
            } catch (e: Exception) {
                throw EngineException(
                    message = "Docker conversion failed: ${e.message}",
                    exitCode = 3,
                    hint = "Ensure Docker is running and the md2pdf image is available"
                )
            }
        }

    private suspend fun ensureDockerImage(): Unit =
        withContext(Dispatchers.IO) {
            // Check if image exists locally
            val checkProcess = ProcessBuilder("docker", "images", "-q", imageName)
                .redirectErrorStream(true)
                .start()

            checkProcess.waitFor()
            val imageExists = checkProcess.inputStream.bufferedReader().readText().trim().isNotEmpty()

            if (!imageExists) {
                pullDockerImage()
            }
        }

    private suspend fun pullDockerImage(): Unit =
        withContext(Dispatchers.IO) {
            log.info { "Pulling Docker image: $imageName" }

            val process = ProcessBuilder("docker", "pull", imageName)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(300, TimeUnit.SECONDS) // 5 minute timeout
            if (!completed) {
                process.destroyForcibly()
                throw RuntimeException("Docker image pull timed out")
            }

            if (process.exitValue() != 0) {
                val output = process.inputStream.bufferedReader().readText()
                throw RuntimeException("Failed to pull Docker image: $output")
            }
        }

    private suspend fun getOrStartContainer(options: Map<String, Any>): String =
        withContext(Dispatchers.IO) {
            reuseContainer = options["reuse"] as? Boolean ?: false

            currentContainer?.let { containerId ->
                if (isContainerRunning(containerId)) {
                    return@withContext containerId
                }
            }

            return@withContext startContainer()
        }

    private suspend fun startContainer(): String =
        withContext(Dispatchers.IO) {
            val containerName = "$containerPrefix-${System.currentTimeMillis()}"

            // Start container with health check capability
            val process = ProcessBuilder(
                "docker", "run", "-d",
                "--name", containerName,
                "--rm",
                imageName
            ).redirectErrorStream(true)
                .start()

            process.waitFor()
            if (process.exitValue() != 0) {
                val output = process.inputStream.bufferedReader().readText()
                throw RuntimeException("Failed to start Docker container: $output")
            }

            val containerId = process.inputStream.bufferedReader().readText().trim()

            // Wait for container to be ready with health check
            waitForContainerHealth(containerId)

            currentContainer = containerId
            return@withContext containerId
        }

    private suspend fun waitForContainerHealth(containerId: String): Unit =
        withContext(Dispatchers.IO) {
            val maxWaitTime = 30.seconds
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < maxWaitTime.inWholeMilliseconds) {
                if (isContainerHealthy(containerId)) {
                    return@withContext
                }
                delay(1000) // Wait 1 second before next check
            }

            throw RuntimeException("Container failed to become healthy within $maxWaitTime")
        }

    private fun isContainerRunning(containerId: String): Boolean {
        return try {
            val process = ProcessBuilder("docker", "ps", "-q", "--filter", "id=$containerId")
                .redirectErrorStream(true)
                .start()

            process.waitFor()
            val output = process.inputStream.bufferedReader().readText().trim()
            output.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun isContainerHealthy(containerId: String): Boolean {
        return try {
            // For this implementation, we'll assume the container is healthy if it's running
            // In a real implementation, you'd check the actual health endpoint
            val process = ProcessBuilder("docker", "exec", containerId, "echo", "health-check")
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(5, TimeUnit.SECONDS)
            completed && process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun executeDockerConversion(
        containerId: String,
        inputFile: File,
        outputFile: File,
        options: Map<String, Any>
    ): Boolean =
        withContext(Dispatchers.IO) {
            // Ensure workspace exists inside container
            ProcessBuilder("docker", "exec", containerId, "mkdir", "-p", "/workspace")
                .redirectErrorStream(true)
                .start()
                .waitFor()

            // Copy input file into container
            // See https://docs.docker.com/engine/reference/commandline/cp/
            val copyIn = ProcessBuilder(
                "docker",
                "cp",
                inputFile.absolutePath,
                "$containerId:/workspace/${inputFile.name}"
            ).redirectErrorStream(true)
                .start()

            copyIn.waitFor()
            if (copyIn.exitValue() != 0) {
                val err = copyIn.inputStream.bufferedReader().readText()
                throw RuntimeException("Failed to copy input file to container: $err")
            }

            // Build conversion command
            val command = buildMd2PdfCommand(inputFile.name, outputFile.name, options)
            val dockerCommand = mutableListOf(
                "docker",
                "exec",
                "-w",
                "/workspace",
                containerId
            )
            dockerCommand.addAll(command)

            // Execute conversion
            val process = ProcessBuilder(dockerCommand)
                .redirectErrorStream(true)
                .start()

            val timeoutSeconds = (options["timeout"] as? Int) ?: 60
            withTimeout(timeoutSeconds.seconds) {
                process.waitFor()
            }

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                throw RuntimeException("Docker md2pdf conversion failed: $output")
            }

            // Copy output file back to host
            val copyOut = ProcessBuilder(
                "docker",
                "cp",
                "$containerId:/workspace/${outputFile.name}",
                outputFile.absolutePath,
            ).redirectErrorStream(true)
                .start()

            copyOut.waitFor()
            if (copyOut.exitValue() != 0) {
                val err = copyOut.inputStream.bufferedReader().readText()
                throw RuntimeException("Failed to copy output file from container: $err")
            }

            // Clean up files inside container when reusing
            ProcessBuilder(
                "docker",
                "exec",
                containerId,
                "rm",
                "-f",
                "/workspace/${inputFile.name}",
                "/workspace/${outputFile.name}"
            ).redirectErrorStream(true).start().waitFor()

            return@withContext outputFile.exists()
        }

    private fun buildMd2PdfCommand(
        inputFileName: String,
        outputFileName: String,
        options: Map<String, Any>
    ): List<String> {
        val command = mutableListOf("md2pdf", inputFileName)

        // Add options
        options["pageSize"]?.let { command.addAll(listOf("--page-size", it.toString())) }
        options["margin"]?.let { command.addAll(listOf("--margin", it.toString())) }
        options["theme"]?.let { command.addAll(listOf("--theme", it.toString())) }
        options["css"]?.let { command.addAll(listOf("--css", it.toString())) }
        options["template"]?.let { command.addAll(listOf("--template", it.toString())) }
        if (options["toc"] == true) {
            command.add("--toc")
        }

        // Add output
        command.addAll(listOf("-o", outputFileName))

        return command
    }

    private suspend fun stopContainer(containerId: String): Unit =
        withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("docker", "stop", containerId)
                    .redirectErrorStream(true)
                    .start()

                process.waitFor(10, TimeUnit.SECONDS)
                currentContainer = null
            } catch (e: Exception) {
                // Log warning but don't fail
            }
        }

    private fun readStdin(): String {
        return System.`in`.bufferedReader().readText()
    }

    private suspend fun downloadContent(url: String): String =
        withContext(Dispatchers.IO) {
            // Simple HTTP client - in production, use a proper HTTP client
            java.net.URL(url).readText()
        }
}
