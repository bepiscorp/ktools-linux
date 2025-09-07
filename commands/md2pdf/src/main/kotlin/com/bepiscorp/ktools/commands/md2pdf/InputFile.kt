package com.bepiscorp.ktools.commands.md2pdf

import java.io.File

/** Represents different types of input files. */
sealed class InputFile {
    abstract val displayName: String
    abstract val name: String
    abstract val path: String

    /** Standard input stream. */
    object Stdin : InputFile() {
        override val displayName = "stdin"
        override val name = "stdin"
        override val path = "-"
    }

    /** Local file system file. */
    data class Local(val file: File) : InputFile() {
        override val displayName = file.path
        override val name = file.name
        override val path = file.path
    }

    /** Remote URL. */
    data class Remote(val url: String) : InputFile() {
        override val displayName = url
        override val name = url.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "remote.md"
        override val path = url
    }
}

/** Custom exceptions for the md2pdf module. */
class ValidationException(
    message: String,
    val hint: String = ""
) : Exception(message)

class EngineException(
    message: String,
    val exitCode: Int,
    val hint: String = ""
) : Exception(message)
