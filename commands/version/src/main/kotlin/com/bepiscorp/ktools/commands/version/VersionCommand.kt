package com.bepiscorp.ktools.commands.version

import com.bepiscorp.ktools.core.cli.JsonCommand
import kotlinx.serialization.Serializable

@Serializable
private data class VersionResponse(val version: String)

/** Prints the project version supplied by Gradle. */
class VersionCommand : JsonCommand(help = "Prints ktools version") {
    override fun run() {
        val v = System.getProperty("ktools.version", "0.0.0")
        respond(VersionResponse(v), VersionResponse.serializer(), v)
    }
}
