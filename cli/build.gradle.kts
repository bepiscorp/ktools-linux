plugins {
    application
}

application {
    mainClass.set("com.bepiscorp.ktools.cli.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":commands:hello"))
    implementation(project(":commands:version"))
}
