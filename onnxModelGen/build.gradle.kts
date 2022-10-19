import com.google.protobuf.gradle.*

plugins {
   // id("com.enpasos.muzero.java-conventions")
    id("com.enpasos.muzero.java-conventions")
    id("idea")
    id("com.google.protobuf") version "0.8.19"
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.21.4")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.11.0")
    implementation("com.google.protobuf:protobuf-java:3.21.4")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("ai.djl:model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl:basicdataset:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-engine:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-model-zoo:0.20.0-SNAPSHOT")
    implementation("ai.djl.pytorch:pytorch-native-cu116:1.12.1-SNAPSHOT")
    implementation("commons-cli:commons-cli:1.4")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

description = "onnxModelGen"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.4"
    }
}
