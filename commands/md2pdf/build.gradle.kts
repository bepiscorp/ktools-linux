dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.mockk:mockk")
    testImplementation("org.testcontainers:junit-jupiter")
}
