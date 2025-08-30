plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    id("org.jetbrains.dokka") version "2.0.0" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "com.bepiscorp.ktools"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.dokka")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    dependencies {
        add("implementation", "io.insert-koin:koin-core:3.5.3")
        add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        add("implementation", "com.github.ajalt.clikt:clikt:4.2.0")
        add("implementation", "io.ktor:ktor-client-core:3.0.0")
        add("implementation", "io.ktor:ktor-client-cio:3.0.0")
        add("implementation", "com.typesafe:config:1.4.3")
        add("implementation", "io.github.config4k:config4k:0.4.2")

        add("testImplementation", kotlin("test"))
        add("testImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
        add("testImplementation", "io.mockk:mockk:1.13.10")
        add("testImplementation", "org.testcontainers:junit-jupiter:1.20.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.2.1").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "google",
                "ij_kotlin_allow_trailing_comma" to "true",
            ),
        )
    }
    kotlinGradle {
        ktlint("1.2.1")
    }
    format("markdown") {
        target("**/*.md")
        prettier().config(mapOf("parser" to "markdown"))
    }
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.ALL
}
