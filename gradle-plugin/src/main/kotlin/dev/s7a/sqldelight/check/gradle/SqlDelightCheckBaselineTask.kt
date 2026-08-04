package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisTrace
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.BaselineEntry
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Generates a sqldelight-check baseline file from the current diagnostics.
 */
@DisableCachingByDefault(because = "The task snapshots current diagnostics into a user-maintained baseline file.")
public abstract class SqlDelightCheckBaselineTask : DefaultTask() {
    /**
     * Log output detail for this task execution.
     */
    @get:Input
    public abstract val logLevel: Property<LogLevel>

    /**
     * SQLDelight source files that can influence diagnostics.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sqlDelightSources: ConfigurableFileCollection

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

    /**
     * Runs rules and writes the current diagnostics to the configured baseline file.
     */
    @TaskAction
    public fun run() {
        val extension = project.extensions.getByType(SqlDelightCheckExtension::class.java)
        val logLevel = logLevel.get()
        val config = extension.toCheckConfig(logLevel, baseline = Baseline.Empty)
        val inputs = SqlDelightProjectResolver(project, project.sqldelightCheckDialectsRegistry()).resolve()
        val ruleRegistry = project.sqldelightCheckRuleRegistry()
        validateConfiguredRules(config, ruleRegistry)
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = inputs.map { input -> input.analysisInput },
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
