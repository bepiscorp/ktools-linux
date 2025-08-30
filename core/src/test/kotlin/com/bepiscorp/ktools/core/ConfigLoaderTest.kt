package com.bepiscorp.ktools.core

import com.bepiscorp.ktools.core.config.ConfigLoader
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe

class ConfigLoaderTest :
    StringSpec({
        "loads default config when files missing" {
            val config = ConfigLoader().load()
            config shouldNotBe null
        }
    })
