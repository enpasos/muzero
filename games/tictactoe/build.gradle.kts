plugins {
    id("com.enpasos.muzero.java-conventions")
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependencyManagement)
    id("idea")
}

apply(plugin = "io.spring.dependency-management")

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("ai.enpasos.muzero.tictactoe.TicTacToe")
    this.archiveClassifier.set("exec")
}
tasks.named<Jar>("jar") {
    this.archiveClassifier.set("")
}

dependencies {
    implementation(project(":platform"))

    implementation(libs.djl.api)

    implementation(libs.springboot.starter)
    testImplementation(libs.springboot.starter.test)


    implementation(libs.postgres)
    implementation(libs.springboot.starter.jpa)
    testImplementation(libs.h2database)


    implementation(libs.commons.lang)
    implementation(libs.commons.csv)
    implementation(libs.commons.math)

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


description = "tictactoe"
group = "${group}.games"
