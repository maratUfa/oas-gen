plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":java:jackson-parser"))
    api("io.projectreactor:reactor-core:3.+")
}
