package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLDelight MySQL dialect metadata.
 */
public val MySqlDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.MySql,
        capabilities = setOf(DialectCapability.MySql),
        sourcePatterns = MySqlDialectSourcePatterns,
    )
