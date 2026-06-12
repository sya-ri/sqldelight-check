package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * HSQL dialect ID.
 */
public val HsqlDialectId: DialectId = DialectId("hsql")

/**
 * SQLDelight HSQL dialect metadata.
 */
public val HsqlDialect: SqlDialect =
    SqlDialect(
        ids = setOf(HsqlDialectId),
        sourcePatterns = HsqlDialectSourcePatterns,
    )
