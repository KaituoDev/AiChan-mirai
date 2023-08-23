//import io.gitlab.arturbosch.detekt.Detekt
//import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

//val kotlinVersion: String by project

plugins {
    val kotlinVersion = "1.9.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.13.2"
//    id("io.gitlab.arturbosch.detekt") version("1.23.1")
}

group = "fun.kaituo.aichanmirai"
version = "0.1.0"

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.xsocket/xSocket
    implementation("org.xsocket:xSocket:2.8.15")
    // https://mvnrepository.com/artifact/com.alibaba/fastjson
    implementation("com.alibaba:fastjson:2.0.39")
    // https://mvnrepository.com/artifact/com.macasaet.fernet/fernet-java8
    implementation("com.macasaet.fernet:fernet-java8:1.5.0")

    val detekt = "1.23.1"
//    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detekt")

}

mirai {
    jvmTarget = JavaVersion.VERSION_17
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

//detekt {
//    toolVersion = "1.23.1"
//    parallel = true
//    config.setFrom(file("config/detekt/detekt.yml"))
//    buildUponDefaultConfig = true
//    autoCorrect = false
//}
//
//tasks.withType<Detekt>().configureEach {
//    jvmTarget = "17"
//}
//
//tasks.withType<DetektCreateBaselineTask>().configureEach {
//    jvmTarget = "17"
//}
