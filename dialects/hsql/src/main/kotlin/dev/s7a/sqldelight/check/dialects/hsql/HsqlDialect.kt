package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * HSQL dialect family.
 */
public val HsqlDialectFamily: DialectFamily = DialectFamily.Named("hsql")

/**
 * HSQL-compatible syntax and behavior.
 */
public val HsqlDialectCapability: DialectCapability = DialectCapability("hsql")

/**
 * SQLDelight HSQL dialect metadata.
 */
public val HsqlDialect: SqlDialect =
    SqlDialect(
        family = HsqlDialectFamily,
        capabilities = setOf(HsqlDialectCapability),
        sourcePatterns = HsqlDialectSourcePatterns,
    )
