plugins {
    id("java")
    id("checkstyle")
    id("com.gradleup.shadow") version "9.4.1"
    id("io.freefair.lombok") version "9.2.0"
    id("maven-publish")
    id("com.gradleup.nmcp")
    id("java-library")
    id("signing")
}

checkstyle {
    toolVersion = "10.12.5"
    configFile = file("config/checkstyle/checkstyle.xml")
    configProperties = mapOf("configDirectory" to file("config/checkstyle"))
    isIgnoreFailures = false
    maxWarnings = 0
}

group = "com.craftlyworks"
version = "1.0-RELEASE"

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


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("configra - A simple collection of Java utilities for managing configurations, MongoDB connections, and Redis operations.")
                url.set("https://github.com/CraftlyWorks/configra")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("CraftlyWorks")
                        name.set("CraftlyWorks")
                        email.set("contact@craftlyworks.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/CraftlyWorks/configra.git")
                    developerConnection.set("scm:git:ssh://github.com/CraftlyWorks/configra.git")
                    url.set("https://github.com/CraftlyWorks/configra")
                }
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        logger.warn("GPG key or password not set! Artifacts will not be signed.")
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username.set(findProperty("centralUsername") as String)
        password.set(findProperty("centralPassword") as String)
    }
}
