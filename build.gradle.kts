import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    id("org.jetbrains.dokka") version "2.0.0" apply false
    id("com.diffplug.spotless") version "7.2.1"
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

        // Logging facade and Log4j2 backend
        add("implementation", "io.github.oshai:kotlin-logging-jvm:7.0.3")
        add("implementation", "org.apache.logging.log4j:log4j-core:2.21.1")
        add("implementation", "org.apache.logging.log4j:log4j-slf4j2-impl:2.21.1")

        add("testImplementation", kotlin("test"))
        add("testImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
        add("testImplementation", "io.mockk:mockk:1.13.10")
        add("testImplementation", "org.testcontainers:junit-jupiter:1.20.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val sourceSets = extensions.getByType<SourceSetContainer>()
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests."
        group = "verification"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
    }
    tasks.named("check") { dependsOn("integrationTest") }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.2.1").editorConfigOverride(
            mapOf(
                "ij_kotlin_allow_trailing_comma" to "true",
            ),
        )
    }
    kotlinGradle {
        ktlint("1.2.1")
    }
    format("markdown") {
        target("**/*.md")

        // Prefer nvm-managed node/npm if available, otherwise resolve from PATH
        fun findExec(name: String): String? =
            System.getenv("PATH")?.split(java.io.File.pathSeparatorChar)?.asSequence()
                ?.map { java.io.File(it, name) }?.firstOrNull { it.exists() && it.canExecute() }
                ?.absolutePath
        val nvmBin = System.getenv("NVM_BIN")?.let { java.io.File(it) }
        val nodeExec =
            nvmBin?.resolve("node")?.takeIf { it.exists() && it.canExecute() }?.absolutePath
                ?: findExec("node")
        val npmExec =
            nvmBin?.resolve("npm")?.takeIf { it.exists() && it.canExecute() }?.absolutePath
                ?: findExec("npm")
        val step = prettier().config(mapOf("parser" to "markdown"))
        if (nodeExec != null && npmExec != null) {
            step.nodeExecutable(nodeExec).npmExecutable(npmExec)
        }
    }
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.ALL
}
