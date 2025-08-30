package com.bepiscorp.ktools.core.di

import com.bepiscorp.ktools.core.config.ConfigLoader
import com.bepiscorp.ktools.core.logging.logger
import io.ktor.client.HttpClient
import org.koin.dsl.module

/** Koin module providing shared services.
 * To wire new services, add single { ... } bindings here.
 */
val coreModule =
    module {
        single { ConfigLoader() }
        single { logger("ktools") }
        single { HttpClient() }
    }
