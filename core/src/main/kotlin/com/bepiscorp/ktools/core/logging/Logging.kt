package com.bepiscorp.ktools.core.logging

import io.github.koog.Koog
import io.github.koog.Logger

/** Supplies structured loggers via Koog. */
fun logger(name: String): Logger = Koog.logger(name)
