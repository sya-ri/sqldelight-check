package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation

/**
 * Receives analysis trace events for optional task-level logging.
 *
 * Implementations should stay side-effect free unless the caller explicitly
 * wants to emit logs or collect debug information.
 */
public interface AnalysisTrace {
    /**
     * Whether the engine should measure rule and shared analysis execution.
     *
     * Implementations should return `false` unless they consume timing events.
     */
    public val collectsPerformanceMetrics: Boolean
        get() = false

    /**
     * Signals the resolved SQLDelight files for a database before rules run.
     */
    public fun databaseFiles(
        database: DatabaseContext,
        files: List<SourceFile>,
    )

    /**
     * Signals the rules that executed for one file after suppression handling.
     */
    public fun fileRules(
        database: DatabaseContext,
        file: SourceFile,
        ruleIds: List<QualifiedRuleId>,
    )

    /**
     * Signals the measured duration of one executed rule.
     */
    public fun ruleTiming(
        database: DatabaseContext,
        file: SourceFile,
        ruleId: QualifiedRuleId,
        durationNanos: Long,
    ) {
    }

    /**
     * Signals the measured duration of a shared analysis phase.
     */
    public fun analysisPhaseTiming(
        database: DatabaseContext,
        file: SourceFile,
        phase: AnalysisPhase,
        durationNanos: Long,
    ) {
    }

    /**
     * Signals that user configuration explicitly mentions a deprecated rule.
     */
    public fun deprecatedRule(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        deprecation: RuleDeprecation,
        enabled: Boolean,
    ) {
    }

    /**
     * Signals that user configuration contains an option not declared by the rule.
     */
    public fun unknownRuleOption(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        optionName: String,
        knownOptionNames: Set<String>,
    ) {
    }

    /**
     * Signals that user configuration explicitly mentions a deprecated rule option.
     */
    public fun deprecatedRuleOption(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        optionName: String,
        deprecation: RuleOptionDeprecation,
    ) {
    }

    /**
     * A trace sink that ignores every event.
     */
    public data object None : AnalysisTrace {
        override fun databaseFiles(
            database: DatabaseContext,
            files: List<SourceFile>,
        ) {
        }

        override fun fileRules(
            database: DatabaseContext,
            file: SourceFile,
            ruleIds: List<QualifiedRuleId>,
        ) {
        }
    }
}
