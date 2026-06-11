package dev.s7a.sqldelight.check.reporter.api

/**
 * Provides reporter instances discovered from built-in modules or external dependencies.
 */
public interface ReporterProvider {
    /**
     * Reporter ID used in the Gradle DSL.
     */
    public val id: ReporterId

    /**
     * Creates a reporter instance.
     */
    public fun create(options: Map<String, String> = emptyMap()): Reporter
}
