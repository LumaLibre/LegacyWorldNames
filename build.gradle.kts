plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "dev.lumas.world"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    paperweight.devBundle("io.canvasmc.canvas", "26.1.2.build.+")
}