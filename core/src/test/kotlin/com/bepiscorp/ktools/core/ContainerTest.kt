package com.bepiscorp.ktools.core

import io.kotest.core.spec.style.StringSpec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class ContainerTest :
    StringSpec({
    "runs container".config(enabled = false) {
        GenericContainer(DockerImageName.parse("alpine:3.18")).use {
            it.start()
        }
    }
})
