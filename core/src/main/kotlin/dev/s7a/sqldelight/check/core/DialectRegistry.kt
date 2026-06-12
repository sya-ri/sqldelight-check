package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import java.util.ServiceLoader

/**
 * Registry of dialect providers discovered from the runtime classpath.
 */
public class DialectRegistry private constructor(
    private val providers: List<SqlDialectProvider>,
) {
    /**
     * Resolves dialect metadata for [coordinate].
     */
    public fun resolve(coordinate: SqlDialectCoordinate): SqlDialect =
        providers.firstNotNullOfOrNull { provider -> provider.resolve(coordinate) }
            ?: SqlDialect()

    /**
     * Returns discovered dialect providers.
     */
    public fun providers(): List<SqlDialectProvider> = providers

    public companion object {
        /**
         * Creates a registry from explicit providers.
         */
        public fun create(providers: List<SqlDialectProvider>): DialectRegistry = DialectRegistry(providers)

        /**
         * Loads dialect providers visible to [classLoader].
         */
        public fun load(classLoader: ClassLoader): DialectRegistry {
            val providers =
                ServiceLoader
                    .load(SqlDialectProvider::class.java, classLoader)
                    .toList()
            return DialectRegistry(providers)
        }
    }
}
