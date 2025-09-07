package com.bepiscorp.ktools.commands.md2pdf

import java.io.File
import java.util.concurrent.TimeUnit

/** Detects and selects available md2pdf engines. */
class EngineDetector {

    /** Detect all available engines and their status. */
    fun detectEngines(): List<EngineInfo> {
        return listOf(
            detectNodeEngine(),
            detectDockerEngine()
        )
    }

    /** Select the best available engine based on preference. */
    fun selectEngine(preference: String): EngineInfo {
        val engines = detectEngines()

        return when (preference.lowercase()) {
            "native" -> {
                val nodeEngine = engines.find { it.name == "native" }
                nodeEngine?.takeIf { it.available }
                    ?: throw IllegalStateException(
                        "Native engine not available: ${nodeEngine?.reason ?: "Node.js not found"}"
                    )
            }
            "docker" -> {
                val dockerEngine = engines.find { it.name == "docker" }
                dockerEngine?.takeIf { it.available }
                    ?: throw IllegalStateException(
                        "Docker engine not available: ${dockerEngine?.reason ?: "Docker not found"}"
                    )
            }
            "auto" -> {
                engines.find { it.available }
                    ?: throw IllegalStateException("No engines available. Install Node.js (≥18) or Docker.")
            }
            else -> throw IllegalArgumentException("Unknown engine: $preference")
        }
    }

    private fun detectNodeEngine(): EngineInfo {
        return try {
            val nodeVersion = executeCommand(listOf("node", "--version"))
            val versionNumber = nodeVersion.removePrefix("v")
            val majorVersion = versionNumber.split(".")[0].toIntOrNull() ?: 0

            if (majorVersion < 18) {
                EngineInfo(
                    name = "native",
                    available = false,
                    version = versionNumber,
                    reason = "Node.js version $versionNumber found, but version ≥18 is required"
                )
            } else {
                // Check if npm is available
                executeCommand(listOf("npm", "--version"))

                EngineInfo(
                    name = "native",
                    available = true,
                    version = versionNumber,
                    path = findExecutable("node") ?: "node"
                )
            }
        } catch (e: Exception) {
            EngineInfo(
                name = "native",
                available = false,
                reason = "Node.js not found or not executable"
            )
        }
    }

    private fun detectDockerEngine(): EngineInfo {
        return try {
            val dockerVersion = executeCommand(listOf("docker", "--version"))
            val version = dockerVersion.substringAfter("Docker version ").substringBefore(",")

            // Test if Docker daemon is running
            executeCommand(listOf("docker", "info"), timeoutSeconds = 5)

            EngineInfo(
                name = "docker",
                available = true,
                version = version,
                path = findExecutable("docker") ?: "docker"
            )
        } catch (e: Exception) {
            EngineInfo(
                name = "docker",
                available = false,
                reason = "Docker not found or daemon not running"
            )
        }
    }

    private fun executeCommand(command: List<String>, timeoutSeconds: Long = 10): String {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
        }

        val exitCode = process.exitValue()
        val output = process.inputStream.bufferedReader().readText().trim()

        if (exitCode != 0) {
            throw RuntimeException("Command failed with exit code $exitCode: $output")
        }

        return output
    }

    private fun findExecutable(name: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSeparator = if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
        val executableExtensions = if (System.getProperty("os.name").lowercase().contains("windows")) {
            listOf("", ".exe", ".cmd", ".bat")
        } else {
            listOf("")
        }

        for (dir in pathEnv.split(pathSeparator)) {
            for (ext in executableExtensions) {
                val executable = File(dir, name + ext)
                if (executable.exists() && executable.canExecute()) {
                    return executable.absolutePath
                }
            }
        }

        return null
    }
}
