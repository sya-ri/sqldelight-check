package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin entry point for sqldelight-check.
 */
public class SqlDelightCheckGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("sqldelightCheck", SqlDelightCheckExtension::class.java)
        target.createRuleSetConfiguration()
        target.createReporterConfiguration()
        target.registerSqlDelightCheckTasks()
    }

    /**
     * Registers the dependency bucket for external rule set providers.
     */
    private fun Project.createRuleSetConfiguration() {
        configurations.create("sqldelightCheckRuleSet") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check rule set provider artifacts."
        }
    }

    /**
     * Registers the dependency bucket for external reporter providers.
     */
    private fun Project.createReporterConfiguration() {
        configurations.create("sqldelightCheckReporter") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check reporter provider artifacts."
        }
    }

    /**
     * Registers aggregate task names modeled after Biome's check/lint/format/write commands.
     */
    private fun Project.registerSqlDelightCheckTasks() {
        val taskGroup = "sqldelight-check"
        tasks.register("sqldelightCheck", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Runs SQLDelight lint and format checks without modifying files."
        }
        tasks.register("sqldelightCheckWrite", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Applies SQLDelight formatter output and safe lint fixes."
        }
        tasks.register("sqldelightLint", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Runs SQLDelight lint checks without modifying files."
        }
        tasks.register("sqldelightLintWrite", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Applies safe SQLDelight lint fixes."
        }
        tasks.register("sqldelightFormat", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Checks SQLDelight formatting without modifying files."
        }
        tasks.register("sqldelightFormatWrite", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Applies SQLDelight formatting."
        }
    }
}

