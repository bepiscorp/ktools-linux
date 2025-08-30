package com.bepiscorp.ktools.core.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

/** Loads configuration files from the user's config directory.
 * Global: ~/.config/ktools/config.conf
 * Per-command: ~/.config/ktools/<command>.conf
 * Missing files are ignored and defaults are used.
 *
 * To extend configs, add new keys in HOCON files and read them via config4k.
 */
class ConfigLoader {
    fun load(command: String? = null): Config {
        val baseDir = File(System.getProperty("user.home"), ".config/ktools")
        val configs = mutableListOf<Config>()

        val globalFile = File(baseDir, "config.conf")
        if (globalFile.exists()) {
            configs += ConfigFactory.parseFile(globalFile)
        }

        if (command != null) {
            val commandFile = File(baseDir, "$command.conf")
            if (commandFile.exists()) {
                configs += ConfigFactory.parseFile(commandFile)
            }
        }

        val merged = configs.fold(ConfigFactory.empty()) { acc, cfg -> cfg.withFallback(acc) }
        return merged.resolve()
    }
}
