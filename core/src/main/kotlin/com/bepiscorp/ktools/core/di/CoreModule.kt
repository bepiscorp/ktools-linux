package com.bepiscorp.ktools.core.di

import com.bepiscorp.ktools.core.config.ConfigLoader
import com.bepiscorp.ktools.core.logging.logger
import io.ktor.client.HttpClient
import org.koin.dsl.module
import io.github.oshai.kotlinlogging.KLogger

/** Koin module providing shared services.
 * To wire new services, add single { ... } bindings here.
 */
val coreModule =
    module {
        single { ConfigLoader() }
        single<KLogger> { logger("ktools") }
        single { HttpClient() }
    }
