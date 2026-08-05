package dev.s7a.sqldelight.check.gradle

import app.cash.sqldelight.gradle.SqlDelightTask
import dev.s7a.sqldelight.check.api.LogLevel
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
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

    private fun Project.createRuleSetConfiguration() {
        configurations.create("sqldelightCheckRuleSet") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check rule set provider artifacts."
        }
    }

    private fun Project.createReporterConfiguration() {
        configurations.create("sqldelightCheckReporter") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check reporter provider artifacts."
        }
    }

    private fun Project.createDialectsConfiguration() {
        configurations.create("sqldelightCheckDialects") { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.description = "External sqldelight-check dialect artifacts."
        }
    }

    private fun Project.registerSqlDelightCheckTasks(extension: SqlDelightCheckExtension) {
        val taskGroup = "sqldelight-check"

        tasks.register("sqldelightCheck", SqlDelightCheckTask::class.java) { task ->
            task.description = "Runs configured SQLDelight rules without modifying files."
            task.applyFixes.convention(false)
            configureCheckFixTaskDefaults(task, extension, taskGroup)
        }.also { sqldelightCheck ->
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task ->
                task.dependsOn(sqldelightCheck)
            }
        }

        tasks.register("sqldelightFix", SqlDelightCheckTask::class.java) { task ->
            task.description = "Applies allowed SQLDelight fixes, re-runs rules, and writes reports."
            task.applyFixes.convention(true)
            configureCheckFixTaskDefaults(task, extension, taskGroup)
        }

        tasks.register("sqldelightCheckBaseline", SqlDelightCheckBaselineTask::class.java) { task ->
            task.group = taskGroup
            task.description = "Generates a sqldelight-check baseline file from current diagnostics."
            task.logLevel.convention(resolveLogLevelOverride(extension))
            task.ruleSetClasspath.from(configurations.named("sqldelightCheckRuleSet"))
            task.dialectClasspath.from(configurations.named("sqldelightCheckDialects"))
            task.baselineFile.convention(
                extension.baselineFile.orElse(layout.projectDirectory.file("sqldelight-check-baseline.txt")),
            )
            task.rootProjectDir.set(rootProject.layout.projectDirectory.asFile.absolutePath)
            configureReportRoot(task)
        }

        // After all build scripts are evaluated, snapshot extension state and SQLDelight
        // task model into task properties. This captures everything the task action needs
        // so task actions run with no project access (configuration cache compatibility).
        afterEvaluate {
            val checkTask = tasks.named("sqldelightCheck", SqlDelightCheckTask::class.java)
            val fixTask = tasks.named("sqldelightFix", SqlDelightCheckTask::class.java)
            val baselineTask = tasks.named("sqldelightCheckBaseline", SqlDelightCheckBaselineTask::class.java)

            val databases = discoverDatabases()

            listOf(checkTask, fixTask).forEach { taskProvider ->
                taskProvider.configure { task ->
                    populateSharedTaskSpecs(task, databases, extension)
                    task.allowUnsafeFixes.set(extension.fix.unsafe.get())
                    populateReporterSpecs(task.reporters, extension)
                }
            }

            baselineTask.configure { task ->
                populateSharedTaskSpecs(task, databases, extension)
            }
        }
    }

    private fun Project.configureCheckFixTaskDefaults(
        task: SqlDelightCheckTask,
        extension: SqlDelightCheckExtension,
        taskGroup: String,
    ) {
        task.group = taskGroup
        task.logLevel.convention(resolveLogLevelOverride(extension))
        task.performanceMetrics.convention(resolvePerformanceMetricsOverride(extension))
        configureTaskClasspaths(task)
        task.reportOutputDirectory.convention(layout.buildDirectory.dir("reports/sqldelight-check"))
        task.baselineFile.convention(extension.baselineFile)
        task.rootProjectDir.set(rootProject.layout.projectDirectory.asFile.absolutePath)
        task.projectDir.set(layout.projectDirectory.asFile.absolutePath)
        configureReportRoot(task)
    }

    private fun Project.configureReportRoot(task: AbstractSqlDelightCheckBaseTask) {
        task.reportRoot.set(
            providers
                .gradleProperty("sqldelightCheck.reportRoot")
                .orElse(providers.environmentVariable("GITHUB_WORKSPACE"))
                .orElse(""),
        )
    }

    private fun Project.populateSharedTaskSpecs(
        task: AbstractSqlDelightCheckBaseTask,
        databases: Map<String, DatabaseDiscovery>,
        extension: SqlDelightCheckExtension,
    ) {
        populateDatabaseSpecs(task.databases, databases)
        populateRuleConfigSpecs(
            globalRuleSets = task.globalRuleSets,
            globalRules = task.globalRules,
            databaseConfigs = task.databaseConfigs,
            extension = extension,
        )
    }

    /**
     * Reads SQLDelight task model and returns per-database metadata.
     * Called at configuration time (afterEvaluate); does not trigger dependency resolution.
     */
    private fun Project.discoverDatabases(): Map<String, DatabaseDiscovery> {
        val result = linkedMapOf<String, DatabaseDiscovery>()

        tasks.withType(SqlDelightTask::class.java).all { sqlDelightTask ->
            val properties = runCatching { sqlDelightTask.properties.get() }.getOrNull() ?: return@all
            val databaseName = properties.className

            val compilationUnit =
                runCatching {
                    sqlDelightTask.compilationUnit.orNull ?: properties.compilationUnits.firstOrNull()
                }.getOrNull() ?: return@all

            val sourceFolders =
                compilationUnit.sourceFolders
                    .filterNot { it.dependency }
                    .map { it.folder }

            val dialectConfig = configurations.findByName("${databaseName}DialectClasspath")
            val dialectCoord = dialectConfig?.declaredDialectCoordinate() ?: ""

            val existing = result[databaseName]
            if (existing != null) {
                result[databaseName] = existing.copy(sourceFolders = existing.sourceFolders + sourceFolders)
            } else {
                result[databaseName] = DatabaseDiscovery(
                    dialectCoordinate = dialectCoord,
                    sourceFolders = sourceFolders,
                )
            }
        }

        return result
    }

    private fun Project.populateDatabaseSpecs(
        listProperty: org.gradle.api.provider.ListProperty<SqlDelightDatabaseSpec>,
        databases: Map<String, DatabaseDiscovery>,
    ) {
        databases.forEach { (name, discovery) ->
            val spec = objects.newInstance(SqlDelightDatabaseSpec::class.java)
            spec.name.set(name)
            spec.dialectCoordinate.set(discovery.dialectCoordinate)
            discovery.sourceFolders.forEach { folder ->
                spec.sourceFiles.from(fileTree(folder) { tree ->
                    tree.include("**/*.sq")
                    tree.include("**/*.sqm")
                })
            }
            listProperty.add(spec)
        }
    }

    private fun Project.populateRuleConfigSpecs(
        globalRuleSets: org.gradle.api.provider.ListProperty<RuleSetConfigSpec>,
        globalRules: org.gradle.api.provider.ListProperty<RuleConfigSpec>,
        databaseConfigs: org.gradle.api.provider.ListProperty<DatabaseConfigSpec>,
        extension: SqlDelightCheckExtension,
    ) {
        extension.ruleSets.forEach { ruleSetExt ->
            val spec = objects.newInstance(RuleSetConfigSpec::class.java)
            spec.id.set(ruleSetExt.name)
            ruleSetExt.enabled.orNull?.let { spec.enabled.set(it) }
            globalRuleSets.add(spec)
        }

        extension.rules.forEach { ruleExt ->
            val spec = objects.newInstance(RuleConfigSpec::class.java)
            spec.id.set(ruleExt.name)
            ruleExt.enabled.orNull?.let { spec.enabled.set(it) }
            spec.severity.set(ruleExt.severity.get().name)
            spec.options.set(ruleExt.options.get())
            globalRules.add(spec)
        }

        extension.databases.forEach { dbExt ->
            val dbSpec = objects.newInstance(DatabaseConfigSpec::class.java)
            dbSpec.name.set(dbExt.name)

            dbExt.ruleSets.forEach { ruleSetExt ->
                val ruleSetSpec = objects.newInstance(RuleSetConfigSpec::class.java)
                ruleSetSpec.id.set(ruleSetExt.name)
                ruleSetExt.enabled.orNull?.let { ruleSetSpec.enabled.set(it) }
                dbSpec.ruleSets.add(ruleSetSpec)
            }

            dbExt.rules.forEach { ruleExt ->
                val ruleSpec = objects.newInstance(RuleConfigSpec::class.java)
                ruleSpec.id.set(ruleExt.name)
                ruleExt.enabled.orNull?.let { ruleSpec.enabled.set(it) }
                ruleSpec.severity.set(ruleExt.severity.get().name)
                ruleSpec.options.set(ruleExt.options.get())
                dbSpec.rules.add(ruleSpec)
            }

            databaseConfigs.add(dbSpec)
        }
    }

    private fun Project.populateReporterSpecs(
        reporters: org.gradle.api.provider.ListProperty<SqlDelightReporterTaskSpec>,
        extension: SqlDelightCheckExtension,
    ) {
        extension.reports.forEach { reporter ->
            val spec = objects.newInstance(SqlDelightReporterTaskSpec::class.java)
            spec.name.set(reporter.name)
            spec.required.set(reporter.required.get())
            spec.primaryOutputFile.set(reporter.outputFile.orNull?.asFile?.absolutePath ?: "")
            spec.outputDirectory.set(reporter.outputDirectory.orNull?.asFile?.absolutePath ?: "")
            spec.options.set(reporter.resolvedOptions())
            reporters.add(spec)
        }
    }

    private fun Project.configureTaskClasspaths(task: SqlDelightCheckTask) {
        task.ruleSetClasspath.from(configurations.named("sqldelightCheckRuleSet"))
        task.reporterClasspath.from(configurations.named("sqldelightCheckReporter"))
        task.dialectClasspath.from(configurations.named("sqldelightCheckDialects"))
    }
}

// ── extension state helpers ───────────────────────────────────────────────────

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

private fun Project.resolvePerformanceMetricsOverride(extension: SqlDelightCheckExtension) =
    providers
        .gradleProperty("sqldelightCheck.performanceMetrics")
        .map { value ->
            when (value.lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw IllegalArgumentException(value)
            }
        }
        .orElse(extension.performanceMetrics)

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

// ── dialect coordinate extraction (no dependency resolution) ─────────────────

/**
 * Returns the declared dialect artifact coordinate as "group:module:version", or an empty
 * string when no direct dialect dependency is found.
 *
 * Reads only declared (not resolved) dependencies to avoid triggering resolution at
 * configuration time.
 */
private fun Configuration.declaredDialectCoordinate(): String {
    val dep =
        dependencies
            .filterIsInstance<ModuleDependency>()
            .firstOrNull { it.group != null }
        ?: return ""
    return "${dep.group.orEmpty()}:${dep.name}:${dep.version.orEmpty()}"
}

// ── internal discovery model ──────────────────────────────────────────────────

private data class DatabaseDiscovery(
    val dialectCoordinate: String,
    val sourceFolders: List<java.io.File>,
)

