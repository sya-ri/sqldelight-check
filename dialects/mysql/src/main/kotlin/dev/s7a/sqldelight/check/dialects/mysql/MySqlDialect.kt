package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * MySQL dialect family.
 */
public val MySqlDialectFamily: DialectFamily = DialectFamily.Named("mysql")

/**
 * MySQL-compatible syntax and behavior.
 */
public val MySqlDialectCapability: DialectCapability = DialectCapability("mysql")

/**
 * SQLDelight MySQL dialect metadata.
 */
public val MySqlDialect: SqlDialect =
    SqlDialect(
        family = MySqlDialectFamily,
        capabilities = setOf(MySqlDialectCapability),
        sourcePatterns = MySqlDialectSourcePatterns,
    )
