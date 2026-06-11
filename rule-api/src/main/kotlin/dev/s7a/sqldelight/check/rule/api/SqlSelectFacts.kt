package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for a SELECT statement.
 */
public class SqlSelectFacts(
    /**
     * Source range from SELECT through the end of the select list.
     */
    public val selectListRange: SourceRange,
    /**
     * Result columns in source order.
     */
    public val resultColumns: List<SqlResultColumnFacts>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSelectFacts &&
            selectListRange == other.selectListRange &&
            resultColumns == other.resultColumns

    override fun hashCode(): Int = 31 * selectListRange.hashCode() + resultColumns.hashCode()

    override fun toString(): String = "SqlSelectFacts(selectListRange=$selectListRange, resultColumns=$resultColumns)"
}
