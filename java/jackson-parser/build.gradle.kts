plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:2.+")
    api("io.projectreactor.netty:reactor-netty:0.+")
}
