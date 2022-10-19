plugins {
    id("com.enpasos.muzero.java-conventions")
    id("org.springframework.boot") version "3.0.0-M5"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("idea")
}

apply(plugin = "io.spring.dependency-management")

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("ai.enpasos.mnist.inference.DJLTicTacToeTest")
}


dependencies {
    implementation(project(":platform"))
    implementation(project(":tictactoe"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ai.djl.pytorch:pytorch-engine:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-native-cu116:1.12.1-SNAPSHOT")
    implementation("ai.djl:model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl:basicdataset:0.20.0-SNAPSHOT")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.20.0-SNAPSHOT")

    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    compileOnly("org.jetbrains:annotations:23.0.0")
}

description = "onnxWithRuntime"
