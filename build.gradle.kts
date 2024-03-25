import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    val kotlinVersion = "1.9.23"
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization").version(kotlinVersion)

    val detektVersion = "1.23.6"
    id("io.gitlab.arturbosch.detekt").version(detektVersion)

    id("org.ajoberstar.grgit").version("5.2.2")

    id("net.mamoe.mirai-console").version("2.16.0")
    id("com.github.johnrengelman.shadow").version("8.1.1")  // FIXME
    id("net.kyori.blossom").version("2.1.0")
}

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
    mavenCentral()
}


dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    val gson_version: String by project
    implementation("com.google.code.gson:gson:$gson_version")

    // https://mvnrepository.com/artifact/com.macasaet.fernet/fernet-java8
    val fernet_version: String by project
    implementation("com.macasaet.fernet:fernet-java8:$fernet_version")

    // https://mvnrepository.com/artifact/org.xsocket/xSocket
    val xsocket_version: String by project
    implementation("org.xsocket:xSocket:$xsocket_version")

    val detektVersion = "1.23.6"
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

}

base {
    group = "${properties["maven_group"]}"
    archivesName = "${properties["archives_base_name"]}"
    version = "${properties["version"]}+${
        if (grgit.status().isClean()) {
            grgit.head().abbreviatedId
        } else {
            "dev"
        }
    }"
    rootProject.version = version
}

mirai {
    jvmTarget = JavaVersion.VERSION_17
}

detekt {
    parallel = true
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

sourceSets {
    main {
        blossom {
            resources {
                property("version", version.toString())
            }
        }
    }
}

tasks {
    val jvmVersion = "17"

    withType<Detekt>().configureEach {
        jvmTarget = jvmVersion
    }

    withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = jvmVersion
    }

    compileKotlin {
        dependsOn("detekt")
    }
}
