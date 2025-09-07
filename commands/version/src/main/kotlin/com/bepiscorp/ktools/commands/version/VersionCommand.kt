package com.bepiscorp.ktools.commands.version

import com.bepiscorp.ktools.core.cli.JsonCommand
import com.bepiscorp.ktools.core.BuildInfo
import kotlinx.serialization.Serializable

@Serializable
private data class VersionResponse(
    val version: String
)

/** Prints the project version supplied by Gradle. */
class VersionCommand : JsonCommand(help = "Prints ktools version") {
    override fun run() {
        val v = BuildInfo.version
        respond(VersionResponse(v), VersionResponse.serializer(), v)
    }
}
