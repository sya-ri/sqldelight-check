package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile

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
    ): Unit

    /**
     * Signals the rules that executed for one file after suppression handling.
     */
    public fun fileRules(
        database: DatabaseContext,
        file: SourceFile,
        ruleIds: List<QualifiedRuleId>,
    ): Unit

    /**
     * A trace sink that ignores every event.
     */
    public data object None : AnalysisTrace {
        override fun databaseFiles(
            database: DatabaseContext,
            files: List<SourceFile>,
        ): Unit = Unit

        override fun fileRules(
            database: DatabaseContext,
            file: SourceFile,
            ruleIds: List<QualifiedRuleId>,
        ): Unit = Unit
    }
}
