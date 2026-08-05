package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisTrace
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.BaselineEntry
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import org.gradle.api.file.RegularFileProperty
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
public abstract class SqlDelightCheckBaselineTask : AbstractSqlDelightCheckBaseTask() {
    /**
     * Baseline file to write.
     */
    @get:OutputFile
    public abstract val baselineFile: RegularFileProperty

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

    private fun tracing(logLevel: LogLevel): AnalysisTrace =
        object : LoggingAnalysisTrace(logLevel, logger) {
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
        }
}
