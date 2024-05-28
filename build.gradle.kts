plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.hazelcast:hazelcast:5.4.0")
}

application {
    mainClass = "org.example.ClientSetTps"
}