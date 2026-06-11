package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.core.ReporterRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import java.net.URLClassLoader
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin entry point for sqldelight-check.
 */
public class SqlDelightCheckGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("sqldelightCheck", SqlDelightCheckExtension::class.java)
        target.configureDefaultReports(extension)
        target.createRuleSetConfiguration()
        target.createReporterConfiguration()
        target.registerSqlDelightCheckTasks(extension)
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
     * Registers check and fix tasks for SQLDelight sources.
     */
    private fun Project.registerSqlDelightCheckTasks(extension: SqlDelightCheckExtension) {
        val taskGroup = "sqldelight-check"
        tasks.register("sqldelightCheck", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Runs configured SQLDelight rules without modifying files."
            task.applyFixes.convention(false)
            task.logLevel.convention(resolveLogLevelOverride(extension))
        }
        tasks.register("sqldelightFix", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Applies allowed SQLDelight fixes, re-runs rules, and writes reports."
            task.applyFixes.convention(true)
            task.logLevel.convention(resolveLogLevelOverride(extension))
        }
    }
}

/**
 * Resolves the effective task log level from the CLI override or extension default.
 */
private fun Project.resolveLogLevelOverride(extension: SqlDelightCheckExtension) =
    providers
        .gradleProperty("sqldelightCheck.logLevel")
        .map { value ->
            when (value.lowercase()) {
                "info" -> LogLevel.Info
                "verbose" -> LogLevel.Verbose
                "debug" -> LogLevel.Debug
                else -> throw IllegalArgumentException(value)
            }
        }
        .orElse(extension.logLevel)

/**
 * Returns the reporter registry attached to this project.
 */
internal fun Project.sqldelightCheckReporterRegistry(): ReporterRegistry =
    ReporterRegistry.load(sqldelightCheckProviderClassLoader("sqldelightCheckReporter"))

/**
 * Registers default reporters and output locations.
 */
private fun Project.configureDefaultReports(extension: SqlDelightCheckExtension) {
    val isGitHubActions =
        providers
            .environmentVariable("GITHUB_ACTIONS")
            .map { value -> value.equals("true", ignoreCase = true) }
            .orElse(false)
    val defaults =
        mapOf(
            "json" to true,
            "sarif" to true,
            "text" to true,
            "html" to false,
            "markdown" to false,
            "github-annotations" to false,
        )
    defaults.forEach { (name, required) ->
        extension.reports.maybeCreate(name).apply {
            this.required.convention(required)
            outputFile.convention(layout.buildDirectory.file("reports/sqldelight-check/report.$name"))
            outputDirectory.convention(layout.buildDirectory.dir("reports/sqldelight-check/$name"))
        }
    }
    extension.reports.maybeCreate("github-annotations").required.convention(isGitHubActions)
}

/**
 * Returns the rule registry attached to this project.
 */
internal fun Project.sqldelightCheckRuleRegistry(): RuleRegistry =
    RuleRegistry.load(sqldelightCheckProviderClassLoader("sqldelightCheckRuleSet"))

private fun Project.sqldelightCheckProviderClassLoader(configurationName: String): ClassLoader {
    val urls =
        configurations
            .getByName(configurationName)
            .files
            .map { file -> file.toURI().toURL() }
            .toTypedArray()
    return URLClassLoader(urls, SqlDelightCheckGradlePlugin::class.java.classLoader)
}
