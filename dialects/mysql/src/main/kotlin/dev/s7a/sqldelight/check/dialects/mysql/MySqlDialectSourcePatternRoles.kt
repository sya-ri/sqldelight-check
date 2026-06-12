package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole

/**
 * Changes an existing column definition.
 */
public data object ColumnChangeOperation : SqlDialectSourcePatternRole

/**
 * Drops a column from an existing table.
 */
public data object ColumnDropOperation : SqlDialectSourcePatternRole

/**
 * Modifies an existing column definition.
 */
public data object ColumnModifyOperation : SqlDialectSourcePatternRole

/**
 * Marks a copy-style alter-table algorithm clause.
 */
public data object CopyAlgorithmClause : SqlDialectSourcePatternRole

/**
 * Marks an exclusive lock clause.
 */
public data object ExclusiveLockClause : SqlDialectSourcePatternRole

/**
 * Marks an integer type that can carry a display width.
 */
public data object IntegerDisplayWidthType : SqlDialectSourcePatternRole

/**
 * Marks a legacy `utf8` character set declaration.
 */
public data object LegacyUtf8CharsetDeclaration : SqlDialectSourcePatternRole

/**
 * Starts a `REPLACE INTO` statement.
 */
public data object ReplaceIntoStatementStart : SqlDialectSourcePatternRole
