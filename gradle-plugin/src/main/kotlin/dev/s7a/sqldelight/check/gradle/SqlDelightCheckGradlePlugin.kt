package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.core.DialectRegistry
import dev.s7a.sqldelight.check.core.ReporterRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Gradle plugin entry point for sqldelight-check.
 */
public class SqlDelightCheckGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(LifecycleBasePlugin::class.java)
        val extension = target.extensions.create("sqldelightCheck", SqlDelightCheckExtension::class.java)
        target.configureDefaultReports(extension)
        target.createRuleSetConfiguration()
        target.createReporterConfiguration()
        target.createDialectsConfiguration()
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
     * Registers the dependency bucket for external dialects.
     */
    private fun Project.createDialectsConfiguration() {
        configurations.create("sqldelightCheckDialects") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check dialect artifacts."
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
            configureTaskInputs(task)
            task.notCompatibleWithConfigurationCache(
                "sqldelight-check resolves the SQLDelight Gradle task model and extension state at execution time.",
            )
        }.also { sqldelightCheck ->
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task ->
                task.dependsOn(sqldelightCheck)
            }
        }
        tasks.register("sqldelightFix", SqlDelightCheckTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Applies allowed SQLDelight fixes, re-runs rules, and writes reports."
            task.applyFixes.convention(true)
            task.logLevel.convention(resolveLogLevelOverride(extension))
            configureTaskInputs(task)
            task.notCompatibleWithConfigurationCache(
                "sqldelight-check resolves the SQLDelight Gradle task model and extension state at execution time.",
            )
        }
        tasks.register("sqldelightCheckBaseline", SqlDelightCheckBaselineTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Generates a sqldelight-check baseline file from current diagnostics."
            task.logLevel.convention(resolveLogLevelOverride(extension))
            configureBaselineTaskInputs(task)
            task.notCompatibleWithConfigurationCache(
                "sqldelight-check resolves the SQLDelight Gradle task model and extension state at execution time.",
            )
        }
    }
}

private fun Project.configureSqlDelightSourceInputs(sources: ConfigurableFileCollection) {
    sources.from(
        provider {
            fileTree(rootProject.projectDir) { tree ->
                tree.include("**/*.sq")
                tree.include("**/*.sqm")
                tree.exclude(".gradle/**")
                tree.exclude("**/build/**")
            }
        },
    )
}

private fun Project.configureRuleAndDialectInputs(
    ruleSetClasspath: ConfigurableFileCollection,
    dialectClasspath: ConfigurableFileCollection,
) {
    ruleSetClasspath.from(configurations.named("sqldelightCheckRuleSet"))
    dialectClasspath.from(configurations.named("sqldelightCheckDialects"))
}

private fun Project.configureReportOutputs(task: SqlDelightCheckTask) {
    task.reportOutputDirectory.convention(layout.buildDirectory.dir("reports/sqldelight-check"))
}

private fun Project.configureTaskInputs(task: SqlDelightCheckTask) {
    val extension = extensions.getByType(SqlDelightCheckExtension::class.java)
    configureSqlDelightSourceInputs(task.sqlDelightSources)
    configureRuleAndDialectInputs(task.ruleSetClasspath, task.dialectClasspath)
    task.reporterClasspath.from(configurations.named("sqldelightCheckReporter"))
    configureReportOutputs(task)
    task.baselineFile.convention(extension.baselineFile)
}

private fun Project.configureBaselineTaskInputs(task: SqlDelightCheckBaselineTask) {
    val extension = extensions.getByType(SqlDelightCheckExtension::class.java)
    configureSqlDelightSourceInputs(task.sqlDelightSources)
    configureRuleAndDialectInputs(task.ruleSetClasspath, task.dialectClasspath)
    task.baselineFile.convention(
        extension.baselineFile.orElse(layout.projectDirectory.file("sqldelight-check-baseline.txt")),
    )
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

/**
 * Returns the dialect registry attached to this project.
 */
internal fun Project.sqldelightCheckDialectsRegistry(): DialectRegistry =
    DialectRegistry.load(sqldelightCheckProviderClassLoader("sqldelightCheckDialects"))

private fun Project.sqldelightCheckProviderClassLoader(configurationName: String): ClassLoader {
    val urls =
        configurations
            .getByName(configurationName)
            .files
            .map { file -> file.toURI().toURL() }
            .toTypedArray()
    return ProviderClassLoader(urls, SqlDelightCheckGradlePlugin::class.java.classLoader)
}

private class ProviderClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent) {
    override fun getResource(name: String): URL? = findResource(name) ?: parent.getResource(name)

    override fun getResources(name: String): Enumeration<URL> {
        val localResources = findResources(name).iterator().asSequence().toList()
        val parentResources = parent.getResources(name).iterator().asSequence().toList()
        return Collections.enumeration(localResources + parentResources)
    }
}
