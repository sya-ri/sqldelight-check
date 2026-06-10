package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

internal const val SQLDELIGHT_GROUP = "app.cash.sqldelight"
internal const val DIALECT_SUFFIX = "-dialect"

/**
 * Converts a Gradle module coordinate into sqldelight-check dialect metadata.
 */
internal fun sqlDialectFromCoordinate(
    group: String,
    module: String,
    version: String?,
): SqlDialect {
    val artifact = "$group:$module"
    val knownDialect = KnownSqlDelightDialect.from(group, module)
    return when (knownDialect) {
        is KnownSqlDelightDialect.SQLite ->
            SqlDialect(
                family = DialectFamily.SQLite,
                displayName = knownDialect.displayName,
                artifact = artifact,
                version = version,
                capabilities = knownDialect.capabilities,
            )
        KnownSqlDelightDialect.MySql ->
            SqlDialect(
                family = DialectFamily.MySql,
                displayName = knownDialect.displayName,
                artifact = artifact,
                version = version,
                capabilities = knownDialect.capabilities,
            )
        KnownSqlDelightDialect.PostgreSql ->
            SqlDialect(
                family = DialectFamily.PostgreSql,
                displayName = knownDialect.displayName,
                artifact = artifact,
                version = version,
                capabilities = knownDialect.capabilities,
            )
        KnownSqlDelightDialect.Hsql ->
            SqlDialect(
                family = DialectFamily.Hsql,
                displayName = knownDialect.displayName,
                artifact = artifact,
                version = version,
                capabilities = knownDialect.capabilities,
            )
        null ->
            SqlDialect(
                family = DialectFamily.Custom,
                displayName = module,
                artifact = artifact,
                version = version,
            )
    }
}

private sealed interface KnownSqlDelightDialect {
    val displayName: String
    val capabilities: Set<DialectCapability>

    data class SQLite(
        override val displayName: String,
    ) : KnownSqlDelightDialect {
        override val capabilities: Set<DialectCapability> = setOf(DialectCapabilities.SQLite)
    }

    data object MySql : KnownSqlDelightDialect {
        override val displayName: String = "MySQL"
        override val capabilities: Set<DialectCapability> = setOf(DialectCapabilities.MySql)
    }

    data object PostgreSql : KnownSqlDelightDialect {
        override val displayName: String = "PostgreSQL"
        override val capabilities: Set<DialectCapability> = setOf(DialectCapabilities.PostgreSql)
    }

    data object Hsql : KnownSqlDelightDialect {
        override val displayName: String = "HSQL"
        override val capabilities: Set<DialectCapability> = setOf(DialectCapabilities.Hsql)
    }

    companion object {
        fun from(
            group: String,
            module: String,
        ): KnownSqlDelightDialect? {
            if (group != SQLDELIGHT_GROUP) return null
            return when (module) {
                "mysql-dialect" -> MySql
                "postgresql-dialect" -> PostgreSql
                "hsql-dialect" -> Hsql
                else ->
                    if (module.startsWith("sqlite-") && module.endsWith(DIALECT_SUFFIX)) {
                        SQLite(displayName = module.removeSuffix(DIALECT_SUFFIX).replace('-', ' '))
                    } else {
                        null
                    }
            }
        }
    }
}
