package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation

/**
 * Receives analysis trace events for optional task-level logging.
 *
 * Implementations should stay side-effect free unless the caller explicitly
 * wants to emit logs or collect debug information.
 */
public interface AnalysisTrace {
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
