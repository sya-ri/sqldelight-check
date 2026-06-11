import org.gradle.api.tasks.GradleBuild

plugins {
    kotlin("jvm") version "2.4.0"
    id("app.cash.sqldelight") version "2.3.2"
    id("dev.s7a.sqldelight.check")
}

val customRuleSetJar by tasks.registering(GradleBuild::class) {
    dir = file("custom-ruleset")
    tasks = listOf("jar")
}

val customReporterJar by tasks.registering(GradleBuild::class) {
    dir = file("custom-reporter")
    tasks = listOf("jar")
}

dependencies {
    add("sqldelightCheckRuleSet", files("custom-ruleset/build/libs/custom-ruleset-1.0.0.jar"))
    add("sqldelightCheckReporter", files("custom-reporter/build/libs/custom-reporter-1.0.0.jar"))
}

tasks.named("sqldelightCheck") {
    dependsOn(customRuleSetJar, customReporterJar)
}

tasks.named("sqldelightFix") {
    dependsOn(customRuleSetJar, customReporterJar)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
            srcDirs("src/main/sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
        }
    }
}

sqldelightCheck {
    rules {
        rule("example:no-select-star") {
            options.put("message", "Custom rules can read string options.")
        }
    }
    reports {
        report("example") {
            required.set(true)
            options.put("title", "Custom sqldelight-check report")
            outputFile.set(layout.buildDirectory.file("reports/sqldelight-check/custom.txt"))
            outputDirectory.set(layout.buildDirectory.dir("reports/sqldelight-check/custom"))
        }
    }
}
