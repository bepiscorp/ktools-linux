package com.bepiscorp.ktools.commands.md2pdf

import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/** Output destination for md2pdf conversion. */
sealed class OutputDestination {
    /** Write output to stdout. */
    object Stdout : OutputDestination()

    /** Write output to a file. */
    data class FileOutput(
        val path: Path
    ) : OutputDestination() {
        constructor(file: File) : this(file.toPath())
        constructor(pathString: String) : this(Path(pathString))

        fun toFile(): File = path.toFile()
    }

    companion object {
        /** Parse a string into an OutputDestination. */
        fun parse(value: String?): OutputDestination? =
            when (value) {
                null -> null
                "-" -> Stdout
                else -> FileOutput(value)
            }
    }
}

/** Configuration options for md2pdf conversion. */
data class Md2PdfOptions(
    val inputs: List<String>,
    val output: File? = null,
    val outputDir: File? = null,
    val outputPattern: String? = null,
    val overwrite: Boolean = false,
    val pageSize: String = "A4",
    val margin: String? = null,
    val css: File? = null,
    val theme: String? = null,
    val template: File? = null,
    val toc: Boolean = false,
    val templateVars: Map<String, String> = emptyMap(),
    val batch: Boolean = false,
    val parallel: Int = Runtime.getRuntime().availableProcessors(),
    val resume: Boolean = false,
    val failFast: Boolean = false,
    val timeout: Int = 60,
    val allowRemote: Boolean = false,
    val dryRun: Boolean = false,
    val metrics: Boolean = false
)

/** Result of md2pdf conversion operation. */
@Serializable
data class ConversionResult(
    val input: String,
    val output: String,
    val success: Boolean,
    val error: String? = null,
    val timeMs: Long? = null,
    val memoryMb: Long? = null
)

/** Overall result of md2pdf processing. */
data class ProcessingResult(
    val success: Boolean,
    val message: String,
    val engine: String,
    val conversions: List<ConversionResult> = emptyList(),
    val exitCode: Int = if (success) 0 else 2,
    val hint: String = ""
)

/** Information about an available engine. */
data class EngineInfo(
    val name: String,
    val available: Boolean,
    val version: String = "",
    val reason: String = "",
    val path: String = ""
)

/** Engine types. */
enum class Engine {
    AUTO,
    NATIVE,
    DOCKER
}
