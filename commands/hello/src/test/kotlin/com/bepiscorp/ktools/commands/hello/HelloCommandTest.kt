package com.bepiscorp.ktools.commands.hello

import com.bepiscorp.ktools.core.di.coreModule
import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.koin.core.context.startKoin

class HelloCommandTest :
    StringSpec({
    beforeSpec { startKoin { modules(coreModule) } }

    "prints greeting" {
        val result = HelloCommand().test("")
        result.stdout shouldContain "Hello from ktools!"
    }
})
