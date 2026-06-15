plugins {
    `java-library`
    kotlin("jvm") version "2.4.0"
}

group = "com.example"
version = "1.0.0"

dependencies {
    compileOnly(files("../../../api/build/libs/api-0.2.1.jar"))
    compileOnly(files("../../../reporter-api/build/libs/reporter-api-0.2.1.jar"))
}
