package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import java.util.ServiceLoader

/**
 * Discovers reporter providers from the runtime classpath.
 */
public class ReporterRegistry(
    private val providers: List<ReporterProvider>,
) {
    init {
        validateReporterProviders(providers)
    }

    /**
     * Finds a reporter provider by DSL ID.
     */
    public fun find(id: String): ReporterProvider? = find(ReporterId(id))

    /**
     * Finds a reporter provider by DSL ID.
     */
    public fun find(id: ReporterId): ReporterProvider? = providers.firstOrNull { provider -> provider.id == id }

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
                    .sortedBy { provider -> provider.id.value },
            )
    }
}

private fun validateReporterProviders(providers: List<ReporterProvider>) {
    val duplicates =
        providers
            .groupBy { provider -> provider.id.value }
            .filterValues { matches -> matches.size > 1 }
            .keys
            .sorted()
    require(duplicates.isEmpty()) {
        "Duplicate sqldelight-check reporter provider ID(s): ${duplicates.joinToString()}"
    }
}
