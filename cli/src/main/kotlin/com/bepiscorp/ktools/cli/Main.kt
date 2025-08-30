package com.bepiscorp.ktools.cli

import com.bepiscorp.ktools.commands.hello.HelloCommand
import com.bepiscorp.ktools.commands.version.VersionCommand
import com.bepiscorp.ktools.core.di.coreModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/** CLI entrypoint. Discovers commands and sets up DI.
 * Future plugin loading will use ServiceLoader here.
 */
fun main(args: Array<String>) {
    startKoin { modules(coreModule) }
    try {
        KTools().subcommands(
            HelloCommand(),
            VersionCommand(),
        ).main(args)
    } finally {
        // Ensure DI context is shut down to avoid resource leaks in long sessions/tests
        stopKoin()
    }
}

class KTools : CliktCommand(name = "ktools", help = "ktools-linux CLI", printHelpOnEmptyArgs = true) {
    override fun run() = Unit
}
