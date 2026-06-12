package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole

/**
 * Marks an auto-increment column attribute.
 */
public data object AutoincrementKeyword : SqlDialectSourcePatternRole

/**
 * Marks an alter-table operation that cannot be applied in place.
 */
public data object ComplexAlterTableOperation : SqlDialectSourcePatternRole

/**
 * Marks a `DO UPDATE` conflict action.
 */
public data object DoUpdateClause : SqlDialectSourcePatternRole

/**
 * Marks a foreign-key pragma value that disables enforcement.
 */
public data object ForeignKeysOffValue : SqlDialectSourcePatternRole

/**
 * Marks a foreign-key pragma value that enables enforcement.
 */
public data object ForeignKeysOnValue : SqlDialectSourcePatternRole

/**
 * Starts a foreign-key pragma statement.
 */
public data object ForeignKeysPragmaStatementStart : SqlDialectSourcePatternRole

/**
 * Starts an `INSERT OR REPLACE` statement.
 */
public data object InsertOrReplaceStatementStart : SqlDialectSourcePatternRole

/**
 * Marks a rowid-primary-key type that is not exact `INTEGER`.
 */
public data object NonIntegerRowidPrimaryKeyType : SqlDialectSourcePatternRole

/**
 * Starts an `ON CONFLICT` clause.
 */
public data object OnConflictClause : SqlDialectSourcePatternRole

/**
 * Starts a `REPLACE INTO` statement.
 */
public data object ReplaceIntoStatementStart : SqlDialectSourcePatternRole

/**
 * Marks a `WITHOUT ROWID` clause.
 */
public data object WithoutRowidClause : SqlDialectSourcePatternRole
