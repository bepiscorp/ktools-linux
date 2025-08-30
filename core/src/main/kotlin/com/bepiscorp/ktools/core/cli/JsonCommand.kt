package com.bepiscorp.ktools.core.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Base command that adds a --json flag for all subcommands. */
abstract class JsonCommand(
    help: String
) : CliktCommand(help = help) {
    private val json by option("--json", help = "Output in JSON").flag()

    protected fun <T> respond(value: T, serializer: kotlinx.serialization.KSerializer<T>, text: String,) {
        if (json) {
            echo(Json.encodeToString(serializer, value))
        } else {
            echo(text)
        }
    }
}
