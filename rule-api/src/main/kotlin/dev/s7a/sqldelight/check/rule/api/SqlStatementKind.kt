package dev.s7a.sqldelight.check.rule.api

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
