plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
    id("io.freefair.lombok") version "9.2.0"
}

group = "com.craftlyworks.mininggame.helper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //---- MongoDB ----//
    implementation("org.mongodb:mongodb-driver-sync:5.6.4")
    //---- Redis ----//
    implementation("io.lettuce:lettuce-core:7.5.1.RELEASE")
    //---- YAML ----//
    implementation("org.yaml:snakeyaml:2.6")
    //---- JetBrains Annotations ----//
    compileOnly("org.jetbrains:annotations:26.1.0")
    //---- Tests ----//
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<JavaCompile> {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )
    }

    shadowJar {
    }

    build {
        dependsOn(shadowJar)
    }
}