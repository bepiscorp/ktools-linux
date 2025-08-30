package com.bepiscorp.ktools.commands.md2pdf

import com.bepiscorp.ktools.core.cli.JsonCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import io.github.oshai.kotlinlogging.KLogger
import java.io.File

@Serializable
private data class Md2PdfResponse(
    val success: Boolean,
    val message: String,
    val engine: String? = null,
    val conversions: List<ConversionResult> = emptyList()
)

/** Convert Markdown files to PDF using md2pdf. */
class Md2PdfCommand :
    JsonCommand(help = "Convert Markdown files to PDF using md2pdf"),
    KoinComponent {

    private val log by inject<KLogger>()

    // Input arguments
    private val inputs by argument(
        help = "Input markdown files, directories, or '-' for stdin. Supports globs and remote URLs with --allow-remote"
    ).multiple(required = true)

    // Output options
    private val output by option("-o", "--output", help = "Output PDF file path. Use '-' for stdout")
        .file()
    private val outputDir by option("-O", "--output-dir", help = "Directory for generated PDFs (batch mode)")
        .file()
    private val outputPattern by option(
        "--output-pattern",
        help = "Naming template using {dir}, {base}, {name}, {ext} (e.g., '{dir}/pdf/{base}.pdf')"
    )
    private val overwrite by option("--overwrite", help = "Overwrite existing outputs").flag()
    private val noClobber by option("--no-clobber", help = "Fail if output exists (default)").flag()

    // Formatting options
    private val pageSize by option("--page-size", help = "Page size (A4, Letter, etc.)")
        .default("A4")
    private val margin by option("--margin", help = "Margins as top,right,bottom,left with CSS units")
    private val css by option("--css", help = "Custom CSS file").file()
    private val theme by option("--theme", help = "Built-in or custom theme preset")
    private val template by option("--template", help = "Custom HTML template file").file()
    private val toc by option("--toc", help = "Generate table of contents").flag()
    private val vars by option("--var", help = "Set template variables as key=val (repeatable)")
        .multiple()

    // Processing options
    private val batch by option("--batch", help = "Explicit batch mode").flag()
    private val parallel by option("--parallel", help = "Concurrent conversions in batch mode")
        .int().default(Runtime.getRuntime().availableProcessors())
    private val resume by option("--resume", help = "Skip conversion when target exists and is up-to-date").flag()
    private val failFast by option("--fail-fast", help = "Stop on first failure").flag()
    private val keepGoing by option("--keep-going", help = "Continue on failures (default)").flag()
    private val timeout by option("--timeout", help = "Conversion timeout in seconds").int().default(60)

    // Engine options
    private val engine by option("--engine", help = "Engine selection: auto|native|docker")
        .choice("auto", "native", "docker").default("auto")

    // Output control
    private val verbose by option("--verbose", help = "Include md2pdf internal logs").flag()
    private val quiet by option("--quiet", help = "Suppress non-essential output").flag()
    private val color by option("--color", help = "Colorized output control")
        .choice("auto", "always", "never").default("auto")
    private val logJson by option("--log-json", help = "Structured JSON logs for CI").flag()
    private val metrics by option("--metrics", help = "Print conversion time and memory usage").flag()
    private val dryRun by option("--dry-run", help = "List planned work without converting").flag()

    // Configuration
    private val config by option("--config", help = "Path to config file").file()

    // Information
    private val about by option("--about", help = "Show engine, versions, active config files").flag()
    private val version by option("--version", help = "Show md2pdf version only").flag()
    private val allowRemote by option("--allow-remote", help = "Allow https:// markdown inputs").flag()

    init {
        subcommands(
            EnginesCommand(),
            ExamplesCommand()
        )
    }

    override fun run() {
        try {
            val processor = Md2PdfProcessor(
                engine = engine,
                verbose = verbose,
                quiet = quiet,
                config = config,
                log = log
            )

            when {
                version -> {
                    val versionInfo = processor.getVersion()
                    respond(
                        Md2PdfResponse(true, versionInfo, processor.getActiveEngine()),
                        Md2PdfResponse.serializer(),
                        versionInfo
                    )
                }
                about -> {
                    val aboutInfo = processor.getAboutInfo()
                    respond(
                        Md2PdfResponse(true, aboutInfo, processor.getActiveEngine()),
                        Md2PdfResponse.serializer(),
                        aboutInfo
                    )
                }
                else -> {
                    val options = Md2PdfOptions(
                        inputs = inputs,
                        output = output,
                        outputDir = outputDir,
                        outputPattern = outputPattern,
                        overwrite = overwrite && !noClobber,
                        pageSize = pageSize,
                        margin = margin,
                        css = css,
                        theme = theme,
                        template = template,
                        toc = toc,
                        templateVars = parseTemplateVars(vars),
                        batch = batch || inputs.size > 1 || inputs.any { isDirectory(it) },
                        parallel = parallel,
                        resume = resume,
                        failFast = failFast && !keepGoing,
                        timeout = timeout,
                        allowRemote = allowRemote,
                        dryRun = dryRun,
                        metrics = metrics
                    )

                    val result = processor.convert(options)

                    if (!result.success && !quiet) {
                        log.error { "ERROR ${result.exitCode}: ${result.message}" }
                        if (result.hint.isNotEmpty()) {
                            log.warn { "Hint: ${result.hint}" }
                        }
                    }

                    respond(
                        Md2PdfResponse(
                            success = result.success,
                            message = result.message,
                            engine = result.engine,
                            conversions = result.conversions
                        ),
                        Md2PdfResponse.serializer(),
                        if (result.success) result.message else "Conversion failed: ${result.message}"
                    )

                    if (!result.success) {
                        kotlin.system.exitProcess(result.exitCode)
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Unexpected error in md2pdf command" }
            if (!quiet) {
                log.error { "ERROR 2: Unexpected error: ${e.message}" }
                log.warn { "Hint: Run with --verbose for more details or report this issue" }
            }
            kotlin.system.exitProcess(2)
        }
    }

    private fun parseTemplateVars(vars: List<String>): Map<String, String> {
        return vars.associate { varStr ->
            val parts = varStr.split("=", limit = 2)
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid --var format: '$varStr'. Expected 'key=value'")
            }
            parts[0] to parts[1]
        }
    }

    private fun isDirectory(path: String): Boolean {
        return path != "-" && !path.startsWith("http") && File(path).isDirectory
    }
}

/** Show available engines and their status. */
class EnginesCommand : CliktCommand(name = "engines", help = "Show available engines and their status") {
    override fun run() {
        val detector = EngineDetector()
        val engines = detector.detectEngines()

        echo("Available engines:")
        engines.forEach { engine ->
            val status = if (engine.available) "✓" else "✗"
            val version = if (engine.version.isNotEmpty()) " (${engine.version})" else ""
            echo("  $status ${engine.name}$version")
            if (!engine.available && engine.reason.isNotEmpty()) {
                echo("    ${engine.reason}")
            }
        }

        val active = detector.selectEngine("auto")
        echo("\nActive engine: ${active.name}")
    }
}

/** Show usage examples. */
class ExamplesCommand : CliktCommand(name = "examples", help = "Show usage examples") {
    override fun run() {
        echo(
            """
Usage Examples:

Basic conversion:
  ktools md2pdf document.md -o document.pdf

Batch convert directory:
  ktools md2pdf docs/ -O output/

Custom theme and CSS:
  ktools md2pdf *.md --theme github --css custom.css

Stdin to stdout:
  echo "# Hello" | ktools md2pdf - -o -

Force Docker engine:
  ktools md2pdf document.md --engine docker

Custom output pattern:
  ktools md2pdf docs/ --output-pattern "{dir}/pdf/{name}.pdf"

With table of contents:
  ktools md2pdf document.md --toc -o document.pdf

Template variables:
  ktools md2pdf doc.md --var title="My Doc" --var author="Me"

Remote URL (with permission):
  ktools md2pdf https://example.com/doc.md --allow-remote

Parallel batch processing:
  ktools md2pdf docs/ --parallel 4 --output-dir pdfs/

Dry run to see what would be processed:
  ktools md2pdf docs/ --dry-run
            """.trimIndent()
        )
    }
}
