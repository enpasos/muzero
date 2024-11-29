

plugins {
    id("com.enpasos.muzero.java-conventions")
    id("idea")
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(libs.javax.annotation.api)

    implementation(libs.protobuf)

    implementation(libs.onnxruntime)

    implementation(libs.bundles.djl)
    runtimeOnly(libs.djl.pytorch.native.cu124) {
        artifact {
            classifier = "win-x86_64"
        }
    }

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

group = "${group}.onnx"



protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.0"
    }
}
