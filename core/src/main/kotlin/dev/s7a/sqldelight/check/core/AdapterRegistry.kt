package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapterProvider
import java.util.ServiceLoader

/**
 * Registry of SQLDelight adapter providers discovered from the runtime classpath.
 */
public class AdapterRegistry private constructor(
    private val providers: List<SqlDelightAdapterProvider>,
) {
    /**
     * Finds the provider that should handle [version].
     */
    public fun find(version: String): SqlDelightAdapterProvider? =
        providers.firstOrNull { provider -> provider.supports(version) }

    public companion object {
        /**
         * Loads adapter providers visible to [classLoader].
         */
        public fun load(classLoader: ClassLoader): AdapterRegistry {
            val providers =
                ServiceLoader
                    .load(SqlDelightAdapterProvider::class.java, classLoader)
                    .toList()
            return AdapterRegistry(providers)
        }
    }
}
