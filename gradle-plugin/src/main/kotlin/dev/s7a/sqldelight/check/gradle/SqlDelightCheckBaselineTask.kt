package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisTrace
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.BaselineEntry
import dev.s7a.sqldelight.check.core.DialectRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Generates a sqldelight-check baseline file from the current diagnostics.
 *
 * All project state is captured into task properties at configuration time so that
 * the task action runs without any `project` access, enabling configuration cache
 * compatibility.
 */
@DisableCachingByDefault(because = "The task snapshots current diagnostics into a user-maintained baseline file.")
public abstract class SqlDelightCheckBaselineTask : DefaultTask() {
    /**
     * Log output detail for this task execution.
     */
    @get:Input
    public abstract val logLevel: Property<LogLevel>

    /**
     * Baseline file to write.
     */
    @get:OutputFile
    public abstract val baselineFile: RegularFileProperty

    /**
     * Runtime classpath used to discover rule set providers.
     */
    @get:Classpath
    public abstract val ruleSetClasspath: ConfigurableFileCollection

    /**
     * Runtime classpath used to discover dialect metadata providers.
     */
    @get:Classpath
    public abstract val dialectClasspath: ConfigurableFileCollection

    // ── database and config inputs ──────────────────────────────────────────

    /**
     * Per-database source files and dialect coordinates, captured at configuration time.
     */
    @get:Nested
    public abstract val databases: ListProperty<SqlDelightDatabaseSpec>

    /**
     * Global rule set configuration, captured at configuration time.
     */
    @get:Nested
    public abstract val globalRuleSets: ListProperty<RuleSetConfigSpec>

    /**
     * Global rule configuration, captured at configuration time.
     */
    @get:Nested
    public abstract val globalRules: ListProperty<RuleConfigSpec>

    /**
     * Database-specific configuration overrides, captured at configuration time.
     */
    @get:Nested
    public abstract val databaseConfigs: ListProperty<DatabaseConfigSpec>

    /**
     * Runs rules and writes the current diagnostics to the configured baseline file.
     */
    @TaskAction
    public fun run() {
        val logLevel = logLevel.get()
        val config =
            buildCheckConfig(
                globalRuleSets = globalRuleSets.get(),
                globalRules = globalRules.get(),
                databaseConfigs = databaseConfigs.get(),
                allowUnsafeFixes = false,
                logLevel = logLevel,
                baseline = Baseline.Empty,
            )

        val dialectRegistry = buildDialectRegistry()
        val analysisInputs = buildAnalysisInputs(dialectRegistry)
        val ruleRegistry = buildRuleRegistry()
        validateConfiguredRules(config, ruleRegistry)

        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = analysisInputs,
                ruleSetProviders = ruleRegistry.providers(),
                config = config,
                trace = tracing(logLevel),
            )
        val entries = diagnostics.mapNotNull(BaselineEntry::from)
        baselineFile.get().asFile.writeSqldelightCheckBaseline(entries)
        val skippedCount = diagnostics.size - entries.size
        logger.lifecycle(
            "Wrote sqldelight-check baseline with {} diagnostic(s) to {}.",
            entries.size,
            baselineFile.get().asFile,
        )
        if (skippedCount > 0) {
            logger.lifecycle(
                "Skipped {} diagnostic(s) without source locations because baseline entries require file, line, and column.",
                skippedCount,
            )
        }
    }

    private fun buildRuleRegistry(): RuleRegistry = buildRuleRegistry(ruleSetClasspath)

    private fun buildDialectRegistry(): DialectRegistry = buildDialectRegistry(dialectClasspath)

    /**
     * Absolute path of the root project directory, for path relativization.
     */
    @get:Internal
    public abstract val rootProjectDir: Property<String>

    /**
     * Override root for report paths. Empty string means "not set".
     */
    @get:Input
    @get:Optional
    public abstract val reportRoot: Property<String>

    private fun buildAnalysisInputs(dialectRegistry: DialectRegistry) =
        buildAnalysisInputs(
            databases = databases.get(),
            reportRoot = reportRoot.orNull,
            rootProjectDir = rootProjectDir.get(),
            dialectRegistry = dialectRegistry,
        )

    private fun tracing(logLevel: LogLevel): AnalysisTrace =
        object : AnalysisTrace {
            override fun databaseFiles(
                database: DatabaseContext,
                files: List<SourceFile>,
            ) {
                if (!logLevel.logsFiles) return
                logger.lifecycle("sqldelight-check [{}] files ({}):", database.name, files.size)
                files.forEach { file ->
                    logger.lifecycle("sqldelight-check [{}]   - {}", database.name, file.path)
                }
            }

            override fun fileRules(
                database: DatabaseContext,
                file: SourceFile,
                ruleIds: List<QualifiedRuleId>,
            ) {
                if (!logLevel.logsRules) return
                logger.lifecycle("sqldelight-check [{}] {} rules ({}):", database.name, file.path, ruleIds.size)
                if (ruleIds.isEmpty()) {
                    logger.lifecycle("sqldelight-check [{}]   - (none)", database.name)
                    return
                }
                ruleIds.forEach { ruleId ->
                    logger.lifecycle("sqldelight-check [{}]   - {}", database.name, ruleId.value)
                }
            }

            override fun deprecatedRule(
                database: DatabaseContext,
                ruleId: QualifiedRuleId,
                deprecation: RuleDeprecation,
                enabled: Boolean,
            ) {
                logger.warn(deprecatedRuleMessage(database, ruleId, deprecation, enabled))
            }

            override fun unknownRuleOption(
                database: DatabaseContext,
                ruleId: QualifiedRuleId,
                optionName: String,
                knownOptionNames: Set<String>,
            ) {
                logger.warn(unknownRuleOptionMessage(database, ruleId, optionName, knownOptionNames))
            }

            override fun deprecatedRuleOption(
                database: DatabaseContext,
                ruleId: QualifiedRuleId,
                optionName: String,
                deprecation: RuleOptionDeprecation,
            ) {
                logger.warn(deprecatedRuleOptionMessage(database, ruleId, optionName, deprecation))
            }
        }
}

