package com.bepiscorp.ktools.commands.version

import com.bepiscorp.ktools.core.di.coreModule
import com.bepiscorp.ktools.core.BuildInfo
import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.koin.core.context.startKoin

class VersionCommandTest :
    StringSpec({
    beforeSpec { startKoin { modules(coreModule) } }

    "prints version" {
        val result = VersionCommand().test("")
        result.stdout shouldContain BuildInfo.version
    }
})
