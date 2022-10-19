plugins {
    id("com.enpasos.muzero.java-conventions")
    id("org.springframework.boot") version "3.0.0-M5"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("idea")
}

apply(plugin = "io.spring.dependency-management")

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("ai.enpasos.muzero.tictactoe.TicTacToe")
}

dependencies {
    implementation(project(":platform"))
    implementation("com.microsoft.onnxruntime:onnxruntime:1.11.0")
    implementation("ai.djl:api:0.20.0-SNAPSHOT")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-collections4:4.4")

    implementation("org.apache.commons:commons-csv:1.9.0")
    compileOnly("org.jetbrains:annotations:23.0.0")
    testCompileOnly("org.jetbrains:annotations:23.0.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

configurations {
    all {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}

description = "go"
