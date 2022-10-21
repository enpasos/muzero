import com.google.protobuf.gradle.*

plugins {
   // id("com.enpasos.muzero.java-conventions")
    id("com.enpasos.muzero.java-conventions")
    id("idea")
    id("com.google.protobuf") version "0.8.19"
}

dependencies {
    implementation(libs.javax.annotation.api)

    implementation(libs.protobuf)

    implementation(libs.onnxruntime)

    implementation(libs.bundles.djl)

    implementation(libs.bundles.commons)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter)
}

description = "onnxModelGen"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.4"
    }
}
