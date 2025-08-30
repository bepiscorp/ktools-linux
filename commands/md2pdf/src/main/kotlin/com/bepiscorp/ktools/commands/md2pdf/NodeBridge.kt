package com.bepiscorp.ktools.commands.md2pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/** Bridge to the Node.js md2pdf library. */
class NodeBridge {

    private val workspaceDir = File(System.getProperty("java.io.tmpdir"), "ktools-md2pdf")
    private val md2pdfDir = File(workspaceDir, "md2pdf")
    private val bridgeScript = File(md2pdfDir, "bridge.js")
    private val nvmBin: String? = System.getenv("NVM_BIN")
    private val nodeExecutable: String = nvmBin?.let { File(it, "node") }?.takeIf { it.exists() && it.canExecute() }?.absolutePath
        ?: "node"
    private val npmExecutable: String = nvmBin?.let { File(it, "npm") }?.takeIf { it.exists() && it.canExecute() }?.absolutePath
        ?: "npm"

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
        if (!md2pdfDir.exists()) {
            md2pdfDir.mkdirs()
        }

        if (!bridgeScript.exists()) {
            createBridgeScript()
        } else {
            // If an older script was written (referencing deprecated 'md2pdf' package), refresh it
            val current = bridgeScript.readText()
            if (current.contains("require('md2pdf')") || current.contains("md2pdf module not found")) {
                createBridgeScript()
            }
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
        if (!File(md2pdfDir, "node_modules/md-to-pdf").exists()) {
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
            val desiredPackageJson =
                """
                {
                  "name": "ktools-md2pdf-bridge",
                  "version": "1.0.0",
                  "private": true,
                  "dependencies": {
                    "md-to-pdf": "^5.2.4"
                  }
                }
                """.trimIndent()

            // If package.json is missing or references the old 'md2pdf' package, overwrite it
            val shouldRewrite = if (!packageJson.exists()) {
                true
            } else {
                val content = packageJson.readText()
                content.contains("\"md2pdf\"") || !content.contains("\"md-to-pdf\"")
            }

            if (shouldRewrite) {
                // Clean previous installs/locks to avoid conflicts
                File(md2pdfDir, "node_modules").takeIf { it.exists() }?.deleteRecursively()
                File(md2pdfDir, "package-lock.json").takeIf { it.exists() }?.delete()
                File(md2pdfDir, "pnpm-lock.yaml").takeIf { it.exists() }?.delete()
                File(md2pdfDir, "yarn.lock").takeIf { it.exists() }?.delete()
                packageJson.writeText(desiredPackageJson)
            }

            // Run npm install (respect nvm if available)
            val process = ProcessBuilder(npmExecutable, "install")
                .directory(md2pdfDir)
                .redirectErrorStream(true)
                .start()

            // Allow extra time for first-time Chromium download (puppeteer)
            val completed = process.waitFor(300, TimeUnit.SECONDS)
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
            val optionsJson = Json.encodeToString(toJsonObject(options))

            val process = ProcessBuilder(
                nodeExecutable,
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

    private fun toJsonObject(map: Map<String, Any>): JsonObject {
        val content = map.mapValues { (_, value) -> anyToJsonElement(value) }
        return JsonObject(content)
    }

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is File -> JsonPrimitive(value.absolutePath)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val m = value as Map<String, Any?>
                JsonObject(m.mapValues { anyToJsonElement(it.value) })
            }
            is Iterable<*> -> {
                JsonPrimitive(value.joinToString(",") { it?.toString() ?: "null" })
            }
            else -> JsonPrimitive(value.toString())
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
