package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile

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
}
