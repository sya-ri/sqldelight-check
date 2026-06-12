package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLDelight HSQL dialect metadata.
 */
public val HsqlDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.Hsql,
        capabilities = setOf(DialectCapability.Hsql),
        sourcePatterns = HsqlDialectSourcePatterns,
    )
