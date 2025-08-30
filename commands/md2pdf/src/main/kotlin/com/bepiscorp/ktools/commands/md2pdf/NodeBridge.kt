package com.bepiscorp.ktools.commands.md2pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/** Bridge to the Node.js md2pdf library. */
class NodeBridge {

    private val workspaceDir = File(System.getProperty("java.io.tmpdir"), "ktools-md2pdf")
    private val md2pdfDir = File(workspaceDir, "md2pdf")
    private val bridgeScript = File(workspaceDir, "bridge.js")

    init {
        ensureWorkspace()
    }

    /** Convert a markdown input to PDF using the Node.js md2pdf library. */
    suspend fun convert(input: InputFile, output: File, options: Map<String, Any>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Ensure md2pdf is installed
                ensureMd2PdfInstalled()

                // Prepare input content
                val inputContent = when (input) {
                    is InputFile.Stdin -> readStdin()
                    is InputFile.Local -> input.file.readText()
                    is InputFile.Remote -> downloadContent(input.url)
                }

                // Create temporary input file if needed
                val tempInputFile = File.createTempFile("md2pdf-input", ".md")
                tempInputFile.writeText(inputContent)

                try {
                    // Prepare options
                    val conversionOptions = mutableMapOf<String, Any>().apply {
                        putAll(options)
                        put("output", output.absolutePath)
                    }

                    // Execute conversion
                    val success = executeConversion(tempInputFile, conversionOptions)

                    return@withContext success
                } finally {
                    tempInputFile.delete()
                }
            } catch (e: Exception) {
                throw EngineException(
                    message = "Native conversion failed: ${e.message}",
                    exitCode = 2,
                    hint = "Ensure Node.js ≥18 is installed and md2pdf dependencies are available"
                )
            }
        }

    private fun ensureWorkspace() {
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }

        if (!bridgeScript.exists()) {
            createBridgeScript()
        }
    }

    private fun createBridgeScript() {
        val resourcePath = "/md2pdf/bridge.js"
        val resourceStream = NodeBridge::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")

        val script = resourceStream.bufferedReader().use { it.readText() }
        bridgeScript.writeText(script)
    }

    private suspend fun ensureMd2PdfInstalled() {
        if (!File(md2pdfDir, "node_modules/md2pdf").exists()) {
            installMd2Pdf()
        }
    }

    private suspend fun installMd2Pdf(): Unit =
        withContext(Dispatchers.IO) {
            // Create package.json for md2pdf installation
            if (!md2pdfDir.exists()) {
                md2pdfDir.mkdirs()
            }

            val packageJson = File(md2pdfDir, "package.json")
            if (!packageJson.exists()) {
                packageJson.writeText(
                    """
                    {
                      "name": "ktools-md2pdf-bridge",
                      "version": "1.0.0",
                      "dependencies": {
                        "md2pdf": "^5.0.0"
                      }
                    }
                    """.trimIndent()
                )
            }

            // Run npm install
            val process = ProcessBuilder("npm", "install")
                .directory(md2pdfDir)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(60, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                throw RuntimeException("npm install timed out")
            }

            if (process.exitValue() != 0) {
                val output = process.inputStream.bufferedReader().readText()
                throw RuntimeException("npm install failed: $output")
            }
        }

    private suspend fun executeConversion(inputFile: File, options: Map<String, Any>): Boolean =
        withContext(Dispatchers.IO) {
            val optionsJson = Json.encodeToString(options)

            val process = ProcessBuilder(
                "node",
                bridgeScript.absolutePath,
                inputFile.absolutePath,
                optionsJson
            ).redirectErrorStream(true)
                .start()

            val timeoutSeconds = (options["timeout"] as? Int) ?: 60
            val completed = withTimeout(timeoutSeconds.seconds) {
                process.waitFor()
            }

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                throw RuntimeException("Node.js conversion failed: $output")
            }

            return@withContext true
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
