import com.google.protobuf.gradle.*
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.enpasos.muzero.java-conventions")
    id("idea")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependencyManagement)
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.springframework.boot")

tasks.named<BootJar>("bootJar") {
        isEnabled = false
}
tasks.named<Jar>("jar") {
    this.archiveClassifier.set("")
}
dependencies {
    implementation(project(":onnx:onnxModelGen"))

    implementation(libs.protobuf)

    implementation(libs.onnxruntime)

    testImplementation(libs.h2database)

    implementation(libs.springboot.starter.jpa)

    implementation(libs.springboot.starter)
    testImplementation(libs.springboot.starter.test)

    implementation(libs.bundles.commons)



    implementation(libs.bundles.djl)

    implementation(libs.gson)
    implementation(libs.jackson.databind)

    implementation(libs.stochasticsimulation)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    implementation(libs.javax.annotation.api)

    compileOnly(libs.jetbrains.annotations)
    testCompileOnly(libs.jetbrains.annotations)

}

configurations {
    all {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}

description = "platform"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.4"
    }
 }
