rootProject.name = "muzero"

include(":platform")
include(":games:go")
include(":games:tictactoe")
include(":games:connect4")
include(":games:pegsolitair")
include(":onnx:onnxModelGen")
include(":onnx:onnxWithRuntime")

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://repo.spring.io/milestone") }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.springframework.boot") {
                useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
            }
        }
    }
}



