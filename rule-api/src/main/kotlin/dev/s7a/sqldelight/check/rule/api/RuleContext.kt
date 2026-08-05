package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlSourceStructure

/**
 * Stable analysis view exposed to custom rules.
 *
 * This type intentionally does not expose SQLDelight compiler internals. Core analysis translates
 * SQLDelight-version-specific structures into sqldelight-check-owned models before rules run.
 */
public interface RuleContext {
    /**
     * Database containing the file being analyzed.
     */
    public val database: DatabaseContext

    /**
     * File being analyzed.
     */
    public val file: SourceFile

    /**
     * Resolved options for the rule currently being executed.
     */
    public val options: RuleOptions

    /**
     * Stable SQL structure facts for the file being analyzed.
     */
    public val facts: SqlFacts

    /**
     * Dialect-aware source structure for the file being analyzed.
     *
     * The result is computed at most once per file when running multiple rules through the engine,
     * so rules that need structural analysis should prefer this over calling
     * [SqlSourceStructure.parse] directly.
     */
    public val sourceStructure: SqlSourceStructure
        get() = SqlSourceStructure.parse(file.content, database.dialect.sourcePatterns)
}
