package com.bepiscorp.ktools.commands.md2pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/** Main processor for md2pdf conversions. */
class Md2PdfProcessor(
    private val engine: String,
    private val verbose: Boolean,
    private val quiet: Boolean,
    private val config: File?,
    private val log: io.github.koog.Logger
) {
    private val engineDetector = EngineDetector()
    private val configLoader = ConfigLoader()
    private var activeEngine: EngineInfo? = null

    /** Get the version of the active md2pdf engine. */
    fun getVersion(): String {
        val engine = getActiveEngineInfo()
        return when (engine.name) {
            "native" -> getNativeVersion()
            "docker" -> getDockerVersion()
            else -> "Unknown"
        }
    }

    /** Get comprehensive information about the environment. */
    fun getAboutInfo(): String {
        val engine = getActiveEngineInfo()
        val config = loadConfiguration()

        return buildString {
            appendLine("md2pdf Environment Information")
            appendLine("=".repeat(35))
            appendLine()
            appendLine("Active Engine: ${engine.name}")
            appendLine("Engine Version: ${engine.version}")
            appendLine("Engine Path: ${engine.path}")
            appendLine()
            appendLine("Available Engines:")
            engineDetector.detectEngines().forEach { eng ->
                val status = if (eng.available) "✓" else "✗"
                appendLine("  $status ${eng.name} ${eng.version}")
                if (!eng.available && eng.reason.isNotEmpty()) {
                    appendLine("    Reason: ${eng.reason}")
                }
            }
            appendLine()
            appendLine("Configuration:")
            appendLine("  Config file: ${config.configFile ?: "None"}")
            appendLine("  Theme: ${config.theme ?: "default"}")
            appendLine("  Page size: ${config.pageSize}")
            appendLine()
            appendLine("Environment:")
            appendLine("  OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            appendLine("  Java: ${System.getProperty("java.version")}")
            appendLine("  Working directory: ${System.getProperty("user.dir")}")
        }
    }

    /** Get the name of the active engine. */
    fun getActiveEngine(): String {
        return getActiveEngineInfo().name
    }

    /** Convert markdown files according to the options. */
    fun convert(options: Md2PdfOptions): ProcessingResult {
        return try {
            // Validate options
            validateOptions(options)

            // Load configuration
            val config = loadConfiguration(options)

            // Select and initialize engine
            val engineInfo = selectEngine()
            if (!quiet) {
                println("Using ${engineInfo.name} mode")
            }

            // Process inputs
            val inputs = runBlocking { expandInputs(options.inputs, options.allowRemote) }
            if (inputs.isEmpty()) {
                return ProcessingResult(
                    success = false,
                    message = "No valid input files found",
                    engine = engineInfo.name,
                    exitCode = 1,
                    hint = "Check input paths and file extensions (.md, .markdown)"
                )
            }

            // Handle dry run
            if (options.dryRun) {
                return handleDryRun(inputs, options, config, engineInfo)
            }

            // Process conversions
            val results = if (options.batch || inputs.size > 1) {
                runBlocking { processBatch(inputs, options, config, engineInfo) }
            } else {
                listOf(runBlocking { processSingle(inputs.first(), options, config, engineInfo) })
            }

            // Generate summary
            val successful = results.count { it.success }
            val failed = results.count { !it.success }
            val totalTime = results.sumOf { it.timeMs ?: 0 }

            val successSuffix = if (successful != 1) "s" else ""
            val failedSuffix = if (failed != 1) "s" else ""
            val message = when {
                failed == 0 -> "Successfully converted $successful file$successSuffix in ${totalTime}ms"
                successful == 0 -> "Failed to convert $failed file$failedSuffix"
                else -> "Converted $successful file$successSuffix, failed $failed in ${totalTime}ms"
            }

            ProcessingResult(
                success = failed == 0,
                message = message,
                engine = engineInfo.name,
                conversions = results,
                exitCode = if (failed == 0) 0 else 2
            )
        } catch (e: ValidationException) {
            ProcessingResult(
                success = false,
                message = e.message ?: "Validation failed",
                engine = getActiveEngine(),
                exitCode = 1,
                hint = e.hint
            )
        } catch (e: EngineException) {
            ProcessingResult(
                success = false,
                message = e.message ?: "Engine error",
                engine = getActiveEngine(),
                exitCode = e.exitCode,
                hint = e.hint
            )
        } catch (e: Exception) {
            log.error { "Unexpected error during conversion: ${e.message}" }
            ProcessingResult(
                success = false,
                message = "Unexpected error: ${e.message}",
                engine = getActiveEngine(),
                exitCode = 2,
                hint = "Run with --verbose for more details"
            )
        }
    }

    private fun getActiveEngineInfo(): EngineInfo {
        if (activeEngine == null) {
            activeEngine = engineDetector.selectEngine(engine)
        }
        return activeEngine!!
    }

    private fun selectEngine(): EngineInfo {
        return try {
            engineDetector.selectEngine(engine)
        } catch (e: Exception) {
            throw EngineException(
                message = e.message ?: "Engine selection failed",
                exitCode = if (engine == "auto") 4 else 3,
                hint = when (engine) {
                    "native" -> "Install Node.js version 18 or later: https://nodejs.org/"
                    "docker" -> "Install Docker and ensure the daemon is running: https://docker.com/"
                    else -> "Install either Node.js (≥18) or Docker"
                }
            )
        }
    }

    private fun validateOptions(options: Md2PdfOptions) {
        // Validate inputs
        if (options.inputs.isEmpty()) {
            throw ValidationException("No input files specified", "Provide at least one input file or directory")
        }

        // Validate conflicting options
        if (
            options.overwrite &&
            options.output == null &&
            options.outputDir == null &&
            options.outputPattern == null
        ) {
            throw ValidationException(
                "--overwrite specified but no output destination",
                "Use -o, -O, or --output-pattern"
            )
        }

        // Validate remote inputs
        val remoteInputs = options.inputs.filter { it.startsWith("http") }
        if (remoteInputs.isNotEmpty() && !options.allowRemote) {
            throw ValidationException(
                "Remote URLs not allowed: ${remoteInputs.first()}",
                "Use --allow-remote to enable remote URL processing"
            )
        }

        // Validate file inputs
        options.inputs.filter { it != "-" && !it.startsWith("http") }.forEach { input ->
            val file = File(input)
            if (!file.exists()) {
                throw ValidationException("Input file not found: $input", "Check the file path and permissions")
            }
            if (file.isFile && !input.endsWith(".md") && !input.endsWith(".markdown")) {
                throw ValidationException(
                    "Unsupported file extension: $input",
                    "Only .md and .markdown files are supported"
                )
            }
        }

        // Validate template file
        options.template?.let { template ->
            if (!template.exists() || !template.isFile) {
                throw ValidationException("Template file not found: ${template.path}", "Check the template file path")
            }
        }

        // Validate CSS file
        options.css?.let { css ->
            if (!css.exists() || !css.isFile) {
                throw ValidationException("CSS file not found: ${css.path}", "Check the CSS file path")
            }
        }
    }

    private fun loadConfiguration(options: Md2PdfOptions? = null): Md2PdfConfig {
        val configFile = options?.let {
            config ?: findConfigFile()
        } ?: findConfigFile()

        return configLoader.load(configFile)
    }

    private fun findConfigFile(): File? {
        // Search order: --config > project local > user config
        val candidates = listOf(
            File(".ktools/md2pdf.yaml"),
            File("ktools.yaml"),
            File(System.getProperty("user.home"), ".config/ktools/md2pdf.yaml"),
            File(System.getProperty("user.home"), ".config/ktools/config.yaml")
        )

        return candidates.find { it.exists() && it.isFile }
    }

    private suspend fun expandInputs(inputs: List<String>, allowRemote: Boolean): List<InputFile> {
        return inputs.flatMap { input ->
            when {
                input == "-" -> listOf(InputFile.Stdin)
                input.startsWith("http") -> {
                    if (allowRemote) listOf(InputFile.Remote(input)) else emptyList()
                }
                File(input).isDirectory -> expandDirectory(File(input))
                input.contains("*") || input.contains("?") -> expandGlob(input)
                else -> listOf(InputFile.Local(File(input)))
            }
        }
    }

    private fun expandDirectory(dir: File): List<InputFile.Local> {
        return dir.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".md") || it.name.endsWith(".markdown")) }
            .map { InputFile.Local(it) }
            .toList()
    }

    private fun expandGlob(pattern: String): List<InputFile.Local> {
        // Basic glob expansion - in a real implementation, you'd use a proper glob library
        val baseDir = File(pattern.substringBeforeLast("/").takeIf { it.isNotEmpty() } ?: ".")
        val filePattern = pattern.substringAfterLast("/")

        return baseDir.walkTopDown()
            .filter { it.isFile && matchesGlob(it.name, filePattern) }
            .filter { it.name.endsWith(".md") || it.name.endsWith(".markdown") }
            .map { InputFile.Local(it) }
            .toList()
    }

    private fun matchesGlob(filename: String, pattern: String): Boolean {
        // Simple glob matching - replace with proper implementation
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return filename.matches(Regex(regex))
    }

    private suspend fun processBatch(
        inputs: List<InputFile>,
        options: Md2PdfOptions,
        config: Md2PdfConfig,
        engineInfo: EngineInfo
    ): List<ConversionResult> {
        val semaphore = Semaphore(options.parallel)
        val results = mutableListOf<ConversionResult>()

        return withContext(Dispatchers.Default) {
            inputs.mapIndexed { index, input ->
                async {
                    semaphore.withPermit {
                        if (!quiet) {
                            println("[${index + 1}/${inputs.size}] Processing ${input.displayName}")
                        }

                        try {
                            processSingle(input, options, config, engineInfo)
                        } catch (e: Exception) {
                            if (options.failFast) {
                                throw e
                            }
                            ConversionResult(
                                input = input.displayName,
                                output = "",
                                success = false,
                                error = e.message
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun processSingle(
        input: InputFile,
        options: Md2PdfOptions,
        config: Md2PdfConfig,
        engineInfo: EngineInfo
    ): ConversionResult {
        val startTime = System.currentTimeMillis()
        val startMemory = if (options.metrics) getUsedMemory() else 0L

        return try {
            val outputFile = determineOutputFile(input, options)

            // Check if resume is enabled and file is up-to-date
            if (options.resume && outputFile.exists() && isUpToDate(input, outputFile)) {
                return ConversionResult(
                    input = input.displayName,
                    output = outputFile.path,
                    success = true,
                    timeMs = 0
                )
            }

            // Perform the actual conversion
            val conversionOptions = buildConversionOptions(options, config)
            val success = when (engineInfo.name) {
                "native" -> runBlocking { convertWithNative(input, outputFile, conversionOptions) }
                "docker" -> runBlocking { convertWithDocker(input, outputFile, conversionOptions) }
                else -> false
            }

            val endTime = System.currentTimeMillis()
            val endMemory = if (options.metrics) getUsedMemory() else 0L

            ConversionResult(
                input = input.displayName,
                output = outputFile.path,
                success = success,
                timeMs = endTime - startTime,
                memoryMb = if (options.metrics) (endMemory - startMemory) / 1024 / 1024 else null
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            ConversionResult(
                input = input.displayName,
                output = "",
                success = false,
                error = e.message,
                timeMs = endTime - startTime
            )
        }
    }

    private fun handleDryRun(
        inputs: List<InputFile>,
        options: Md2PdfOptions,
        config: Md2PdfConfig,
        engineInfo: EngineInfo
    ): ProcessingResult {
        val message = buildString {
            appendLine("Dry run - would process ${inputs.size} file(s)")
            appendLine()
            appendLine("Engine: ${engineInfo.name}")
            appendLine("Parallel: ${options.parallel}")
            appendLine("Batch mode: ${options.batch}")
            appendLine()
            appendLine("Files to process:")
            inputs.forEach { input ->
                val outputFile = determineOutputFile(input, options)
                appendLine("  ${input.displayName} -> ${outputFile.path}")
            }
        }

        return ProcessingResult(
            success = true,
            message = message,
            engine = engineInfo.name
        )
    }

    private fun determineOutputFile(input: InputFile, options: Md2PdfOptions): File {
        return when {
            options.output != null -> options.output
            options.outputPattern != null -> resolveOutputPattern(input, options.outputPattern)
            options.outputDir != null -> File(options.outputDir, changeExtension(input.name, "pdf"))
            else -> File(changeExtension(input.path, "pdf"))
        }
    }

    private fun resolveOutputPattern(input: InputFile, pattern: String): File {
        val inputFile = when (input) {
            is InputFile.Local -> input.file
            else -> File(input.name)
        }

        val dir = inputFile.parent ?: "."
        val name = inputFile.nameWithoutExtension
        val base = inputFile.name
        val ext = inputFile.extension

        val resolved = pattern
            .replace("{dir}", dir)
            .replace("{name}", name)
            .replace("{base}", base)
            .replace("{ext}", ext)

        return File(resolved)
    }

    private fun changeExtension(path: String, newExt: String): String {
        val lastDot = path.lastIndexOf('.')
        return if (lastDot >= 0) {
            path.substring(0, lastDot) + ".$newExt"
        } else {
            "$path.$newExt"
        }
    }

    private fun isUpToDate(input: InputFile, output: File): Boolean {
        if (!output.exists()) return false

        return when (input) {
            is InputFile.Local -> input.file.lastModified() <= output.lastModified()
            else -> false // Always convert stdin and remote files
        }
    }

    private fun buildConversionOptions(options: Md2PdfOptions, config: Md2PdfConfig): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("pageSize", options.pageSize)
            options.margin?.let { put("margin", it) }
            options.css?.let { put("css", it.absolutePath) }
            options.theme?.let { put("theme", it) }
            options.template?.let { put("template", it.absolutePath) }
            put("toc", options.toc)
            put("timeout", options.timeout)
            putAll(options.templateVars)

            // Merge with config
            config.theme?.let { put("theme", it) }
            config.pageSize?.let { put("pageSize", it) }
            config.margin?.let { put("margin", it) }
        }
    }

    private suspend fun convertWithNative(input: InputFile, output: File, options: Map<String, Any>): Boolean {
        return NodeBridge().convert(input, output, options)
    }

    private suspend fun convertWithDocker(input: InputFile, output: File, options: Map<String, Any>): Boolean {
        return DockerBridge().convert(input, output, options)
    }

    private fun getNativeVersion(): String {
        return try {
            val process = ProcessBuilder("npm", "list", "md2pdf", "--depth=0")
                .redirectErrorStream(true)
                .start()

            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()

            // Parse npm list output to extract version
            val versionRegex = Regex("md2pdf@([0-9.]+)")
            versionRegex.find(output)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getDockerVersion(): String {
        return try {
            val process = ProcessBuilder("docker", "images", "realdennis/md2pdf", "--format", "{{.Tag}}")
                .redirectErrorStream(true)
                .start()

            process.waitFor()
            val output = process.inputStream.bufferedReader().readText().trim()
            output.lines().firstOrNull() ?: "latest"
        } catch (e: Exception) {
            "latest"
        }
    }

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
