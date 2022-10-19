import com.google.protobuf.gradle.*
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.enpasos.muzero.java-conventions")
    id("idea")
    id("com.google.protobuf") version "0.8.19"
    id("org.springframework.boot") version "3.0.0-M5"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
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
    implementation(project(":onnxModelGen"))

    implementation("com.google.protobuf:protobuf-java:3.21.4")

    implementation("com.microsoft.onnxruntime:onnxruntime:1.11.0")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("ai.djl:model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl:basicdataset:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-engine:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-native-cu116:1.12.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("commons-cli:commons-cli:1.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("ca.umontreal.iro.simul:ssj:3.3.1")
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.jetbrains:annotations:23.0.0")

}

description = "platform"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.4"
    }
 }
