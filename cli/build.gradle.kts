plugins {
    application
}

application {
    mainClass.set("com.bepiscorp.ktools.cli.MainKt")
    applicationDefaultJvmArgs = listOf("-Dktools.version=${'$'}{project.version}")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":commands:hello"))
    implementation(project(":commands:version"))
}
