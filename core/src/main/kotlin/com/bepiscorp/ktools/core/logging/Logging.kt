package com.bepiscorp.ktools.core.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KLogger

/** Supplies structured loggers via kotlin-logging. */
fun logger(name: String): KLogger = KotlinLogging.logger(name)
