package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable SQL structure facts exposed to custom rules.
 *
 * The model is owned by sqldelight-check so rules do not depend on SQLDelight
 * compiler or IntelliJ PSI classes. Core analysis may populate these facts from
 * SQLDelight, SQL-PSI, or a conservative source scanner depending on adapter
 * support for the current SQLDelight version.
 */
public data class SqlFacts(
    /**
     * Top-level SQL statements discovered in the analyzed source file.
     */
    public val statements: List<SqlStatementFacts> = emptyList(),
)

/**
 * Stable facts for one top-level SQL statement.
 */
public data class SqlStatementFacts(
    /**
     * Statement kind inferred from the leading SQL keyword.
     */
    public val kind: SqlStatementKind,
    /**
     * Source range covered by this statement.
     */
    public val range: SourceRange,
    /**
     * SELECT-specific facts when this statement is a SELECT.
     */
    public val select: SqlSelectFacts? = null,
    /**
     * Table references visible at the statement's top level.
     */
    public val tableReferences: List<SqlTableReferenceFacts> = emptyList(),
    /**
     * JOIN clauses visible at the statement's top level.
     */
    public val joins: List<SqlJoinFacts> = emptyList(),
    /**
     * Qualified column-like references discovered in the statement.
     */
    public val qualifiedReferences: List<SqlQualifiedReferenceFacts> = emptyList(),
)

/**
 * Coarse statement kinds used by parser-backed rules.
 */
public enum class SqlStatementKind {
    /**
     * SELECT or WITH statement that produces rows.
     */
    Select,
    /**
     * INSERT statement.
     */
    Insert,
    /**
     * UPDATE statement.
     */
    Update,
    /**
     * DELETE statement.
     */
    Delete,
    /**
     * CREATE statement.
     */
    Create,
    /**
     * ALTER statement.
     */
    Alter,
    /**
     * DROP statement.
     */
    Drop,
    /**
     * Statement kind not represented by a dedicated enum value.
     */
    Other,
}

/**
 * Stable facts for a SELECT statement.
 */
public data class SqlSelectFacts(
    /**
     * Source range from SELECT through the end of the select list.
     */
    public val selectListRange: SourceRange,
    /**
     * Result columns in source order.
     */
    public val resultColumns: List<SqlResultColumnFacts>,
)

/**
 * Stable facts for a SELECT result column.
 */
public data class SqlResultColumnFacts(
    /**
     * Source range covered by the result column expression and optional alias.
     */
    public val range: SourceRange,
    /**
     * Alias text when the result column declares one.
     */
    public val alias: String? = null,
    /**
     * True when the result column is a wildcard such as `*` or `table.*`.
     */
    public val wildcard: Boolean = false,
)

/**
 * Stable facts for a table or derived-table reference.
 */
public data class SqlTableReferenceFacts(
    /**
     * Source range covered by the table reference.
     */
    public val range: SourceRange,
    /**
     * Referenced table name when the source is a named table.
     */
    public val name: String? = null,
    /**
     * Alias declared for this table reference.
     */
    public val alias: String? = null,
    /**
     * True when this reference is a derived table or subquery.
     */
    public val subquery: Boolean = false,
)

/**
 * Stable facts for a JOIN clause.
 */
public data class SqlJoinFacts(
    /**
     * Source range covered by the JOIN keyword and joined source.
     */
    public val range: SourceRange,
    /**
     * Join kind text as it appears before the JOIN keyword.
     */
    public val kind: String,
    /**
     * Joined table or derived-table reference.
     */
    public val table: SqlTableReferenceFacts,
)

/**
 * Stable facts for a qualified reference such as `table.column`.
 *
 * The model records source syntax only. It does not claim that the qualifier
 * resolves to a table in scope or that the name resolves to a real column.
 */
public data class SqlQualifiedReferenceFacts(
    /**
     * Source range covered by the whole qualified reference.
     */
    public val range: SourceRange,
    /**
     * Qualifier text before the dot.
     */
    public val qualifier: String,
    /**
     * Referenced name after the dot.
     */
    public val name: String,
)
