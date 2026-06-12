package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole

/**
 * Marks a database or file setting statement.
 */
public data object DatabaseFileSettingStatement : SqlDialectSourcePatternRole

/**
 * Marks a system operation statement.
 */
public data object SystemOperationStatement : SqlDialectSourcePatternRole

/**
 * Starts a text-table source binding statement.
 */
public data object TextTableSourceBindingStart : SqlDialectSourcePatternRole

/**
 * Marks a text-table source clause.
 */
public data object TextTableSourceClause : SqlDialectSourcePatternRole

/**
 * Marks a text-table source statement.
 */
public data object TextTableSourceStatement : SqlDialectSourcePatternRole
