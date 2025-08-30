plugins {
    application
}

application {
    mainClass.set("com.bepiscorp.ktools.cli.MainKt")
}

// Ensure stdin is forwarded when running via `./gradlew :cli:run`
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

dependencies {
    implementation(project(":core"))
    implementation(project(":commands:hello"))
    implementation(project(":commands:version"))
}
