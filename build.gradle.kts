import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    val kotlinVersion = "1.9.10"
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization").version(kotlinVersion)

    val detektVersion = "1.23.1"
    id("io.gitlab.arturbosch.detekt").version(detektVersion)

    id("org.ajoberstar.grgit").version("5.2.0")

    id("net.mamoe.mirai-console").version("2.15.0")
    id("com.github.johnrengelman.shadow").version("8.1.1")  // FIXME
}

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    mavenCentral()
}


dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:${property("gson_version")}")

    // https://mvnrepository.com/artifact/com.macasaet.fernet/fernet-java8
    implementation("com.macasaet.fernet:fernet-java8:${property("fernet_version")}")

    // https://mvnrepository.com/artifact/org.xsocket/xSocket
    implementation("org.xsocket:xSocket:${property("xsocket_version")}")

    val detektVersion = "1.23.1"
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

}

base {
    archivesName = "${property("archives_base_name")}"


    val ENV = System.getenv()


    version = "${property("version")}+build.${
        if (ENV["GITHUB_RUN_NUMBER"].toBoolean()) {
            ENV["GITHUB_RUN_NUMBER"]
        } else {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        }
    }+${
        if (grgit.status().isClean()) {
            grgit.head().abbreviatedId
        } else {
            "uncommitted"
        }
    }"

}

mirai {
    jvmTarget = JavaVersion.VERSION_17
}

detekt {
    toolVersion = "1.23.1"
    parallel = true
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}
