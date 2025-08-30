package com.bepiscorp.ktools.commands.hello

import com.bepiscorp.ktools.core.cli.JsonCommand
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
private data class HelloResponse(
    val message: String
)

/** Example hello command. */
class HelloCommand :
    JsonCommand(help = "Prints greeting"),
    KoinComponent {
    private val log by inject<io.github.koog.Logger>()

    override fun run() {
        log.info { "hello invoked" }
        respond(HelloResponse("Hello from ktools!"), HelloResponse.serializer(), "Hello from ktools!")
    }
}
