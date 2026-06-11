package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.util.ServiceLoader

/**
 * Discovers reporter providers from the runtime classpath.
 */
public class ReporterRegistry(
    private val providers: List<ReporterProvider>,
) {
    /**
     * Finds a reporter provider by DSL ID.
     */
    public fun find(id: String): ReporterProvider? = providers.firstOrNull { provider -> provider.id == id }

    /**
     * Returns all discovered reporter providers.
     */
    public fun all(): List<ReporterProvider> = providers

    public companion object {
        /**
         * Loads reporter providers through Java `ServiceLoader`.
         */
        public fun load(classLoader: ClassLoader = Thread.currentThread().contextClassLoader): ReporterRegistry =
            ReporterRegistry(
                ServiceLoader
                    .load(ReporterProvider::class.java, classLoader)
                    .toList()
                    .sortedBy { provider -> provider.id },
            )
    }
}
