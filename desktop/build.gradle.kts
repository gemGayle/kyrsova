plugins {
    id("java")
}

group = "com.caw"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":core"))
    api("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.1")
    api("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-desktop")
    api("com.badlogicgames.gdx:gdx-box2d-platform:1.13.1:natives-desktop")
}

tasks.test {
    useJUnitPlatform()
}
