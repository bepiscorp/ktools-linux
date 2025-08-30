package com.bepiscorp.ktools.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MockTest : StringSpec({
    "mockk works" {
        val service = mockk<() -> String>()
        every { service.invoke() } returns "ok"
        service() shouldBe "ok"
    }
})
