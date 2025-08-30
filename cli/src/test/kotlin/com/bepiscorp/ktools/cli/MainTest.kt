package com.bepiscorp.ktools.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class MainTest : StringSpec({
    "prints help when no args" {
        val out = captureStdout {
            main(arrayOf())
        }
        out.contains("ktools-linux CLI") shouldBe true
    }

    "prints version from VersionCommand" {
        val out = captureStdout {
            main(arrayOf("version"))
        }
        out.trim().isNotEmpty() shouldBe true
    }
})

private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    System.setOut(ps)
    return try {
        block()
        ps.flush()
        baos.toString()
    } finally {
        System.setOut(original)
    }
}
