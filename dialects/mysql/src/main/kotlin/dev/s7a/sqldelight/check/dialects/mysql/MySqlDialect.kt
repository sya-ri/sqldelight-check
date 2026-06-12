package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * MySQL dialect ID.
 */
public val MySqlDialectId: DialectId = DialectId("mysql")

/**
 * SQLDelight MySQL dialect metadata.
 */
public val MySqlDialect: SqlDialect =
    SqlDialect(
        ids = setOf(MySqlDialectId),
        sourcePatterns = MySqlDialectSourcePatterns,
    )
