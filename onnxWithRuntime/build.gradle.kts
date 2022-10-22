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
    implementation(project(":games:tictactoe"))

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.bundles.djl)

    implementation(libs.djl.onnxruntime.engine)


    implementation(libs.springboot.starter)
    testImplementation(libs.springboot.starter.test)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    compileOnly(libs.jetbrains.annotations)
    testCompileOnly(libs.jetbrains.annotations)
}

configurations {
    all {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}

description = "onnxWithRuntime"
