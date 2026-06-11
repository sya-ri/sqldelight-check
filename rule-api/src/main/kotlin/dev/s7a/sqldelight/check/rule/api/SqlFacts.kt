package dev.s7a.sqldelight.check.rule.api

/**
 * Stable SQL structure facts exposed to custom rules.
 *
 * The model is owned by sqldelight-check so rules do not depend on SQLDelight
 * compiler or IntelliJ PSI classes. Core analysis may populate these facts from
 * SQLDelight, SQL-PSI, or a conservative source scanner depending on adapter
 * support for the current SQLDelight version.
 */
public class SqlFacts(
    /**
     * Top-level SQL statements discovered in the analyzed source file.
     */
    public val statements: List<SqlStatementFacts> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlFacts &&
            statements == other.statements

    override fun hashCode(): Int = statements.hashCode()

    override fun toString(): String = "SqlFacts(statements=$statements)"
}
