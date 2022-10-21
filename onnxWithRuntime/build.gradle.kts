plugins {
    id("com.enpasos.muzero.java-conventions")
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependencyManagement)
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

    implementation(libs.springboot.starter)
    testImplementation(libs.springboot.starter.test)

    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    compileOnly("org.jetbrains:annotations:23.0.0")
}

configurations {
    all {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}

description = "onnxWithRuntime"
