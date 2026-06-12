package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole

/**
 * Drops a column from an existing table.
 */
public data object ColumnDropOperation : SqlDialectSourcePatternRole

/**
 * Renames a column in an existing table.
 */
public data object ColumnRenameOperation : SqlDialectSourcePatternRole

/**
 * Changes an existing column type.
 */
public data object ColumnTypeChangeOperation : SqlDialectSourcePatternRole

/**
 * Marks a `CONCURRENTLY` clause.
 */
public data object ConcurrentlyClause : SqlDialectSourcePatternRole

/**
 * Starts a concurrent `CREATE INDEX` statement.
 */
public data object CreateConcurrentIndexStatementStart : SqlDialectSourcePatternRole

/**
 * Marks a `NOT VALID` constraint clause.
 */
public data object NotValidConstraintClause : SqlDialectSourcePatternRole

/**
 * Starts a `REINDEX` statement.
 */
public data object ReindexStatementStart : SqlDialectSourcePatternRole

/**
 * Marks a `REINDEX SYSTEM` target.
 */
public data object ReindexSystemTarget : SqlDialectSourcePatternRole

/**
 * Marks a serial-style generated integer type.
 */
public data object SerialDataTypeName : SqlDialectSourcePatternRole

/**
 * Renames an existing table.
 */
public data object TableRenameOperation : SqlDialectSourcePatternRole

/**
 * Marks a volatile default function.
 */
public data object VolatileDefaultFunction : SqlDialectSourcePatternRole
