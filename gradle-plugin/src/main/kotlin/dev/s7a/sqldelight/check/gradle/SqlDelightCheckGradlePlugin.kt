package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.core.AdapterRegistry
import dev.s7a.sqldelight.check.core.ReporterRegistry
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * Gradle plugin entry point for sqldelight-check.
 */
public class SqlDelightCheckGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("sqldelightCheck", SqlDelightCheckExtension::class.java)
        target.configureDefaultReports(extension)
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

/**
 * Returns the reporter registry attached to this project.
 */
internal fun Project.sqldelightCheckReporterRegistry(): ReporterRegistry =
    (this as ExtensionAware)
        .extensions
        .extraProperties
        .get("sqldelightCheckReporterRegistry") as ReporterRegistry

/**
 * Registers default reporters and output locations.
 */
private fun Project.configureDefaultReports(extension: SqlDelightCheckExtension) {
    val defaults =
        mapOf(
            "json" to true,
            "sarif" to true,
            "text" to true,
            "html" to false,
            "markdown" to false,
        )
    defaults.forEach { (name, required) ->
        extension.reports.maybeCreate(name).apply {
            this.required.convention(required)
            outputFile.convention(layout.buildDirectory.file("reports/sqldelight-check/report.$name"))
        }
    }
    extensions.extraProperties["sqldelightCheckReporterRegistry"] =
        ReporterRegistry.load(SqlDelightCheckGradlePlugin::class.java.classLoader)
    extensions.extraProperties["sqldelightCheckAdapterRegistry"] =
        AdapterRegistry.load(SqlDelightCheckGradlePlugin::class.java.classLoader)
}

/**
 * Returns the adapter registry attached to this project.
 */
internal fun Project.sqldelightCheckAdapterRegistry(): AdapterRegistry =
    (this as ExtensionAware)
        .extensions
        .extraProperties
        .get("sqldelightCheckAdapterRegistry") as AdapterRegistry
