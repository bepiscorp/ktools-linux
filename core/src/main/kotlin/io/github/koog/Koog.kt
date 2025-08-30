package io.github.koog

/** Minimal placeholder for Koog logger.
 * Replace with real library when available.
 */
class Logger(
    private val name: String
) {
    fun info(msg: () -> String) = println("[INFO] [$name] ${'$'}{msg()}")

    fun error(msg: () -> String) = println("[ERROR] [$name] ${'$'}{msg()}")
}

object Koog {
    fun logger(name: String) = Logger(name)
}
