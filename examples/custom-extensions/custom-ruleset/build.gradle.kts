plugins {
    `java-library`
    kotlin("jvm") version "2.4.0"
}

group = "com.example"
version = "1.0.0"

dependencies {
    compileOnly(files("../../../api/build/libs/api-0.1.1.jar"))
    compileOnly(files("../../../rule-api/build/libs/rule-api-0.1.1.jar"))
}
