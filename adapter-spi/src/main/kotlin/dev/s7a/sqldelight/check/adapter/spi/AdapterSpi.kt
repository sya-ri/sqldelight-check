package dev.s7a.sqldelight.check.adapter.spi

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Input required by a SQLDelight-version adapter to analyze one database.
 */
public data class AnalysisInput(
    /** Database metadata resolved by the Gradle plugin. */
    public val database: DatabaseContext,
    /** Source files that belong to the database. */
    public val files: List<SourceFile>,
)

/**
 * Stable analysis result produced by an adapter.
 */
public data class AnalysisResult(
    /** Files accepted for downstream rule and formatter execution. */
    public val files: List<SourceFile>,
    /** Diagnostics emitted by SQLDelight or adapter validation. */
    public val diagnostics: List<Diagnostic>,
)

/**
 * Adapter for one SQLDelight compiler API compatibility range.
 */
public interface SqlDelightAdapter {
    /**
     * Analyzes the given SQLDelight database input.
     */
    public fun analyze(input: AnalysisInput): AnalysisResult
}

/**
 * Provides adapters for a SQLDelight version.
 */
public interface SqlDelightAdapterProvider {
    /** Human-readable provider ID. */
    public val id: String

    /** Exact or range-like SQLDelight versions supported by this provider. */
    public val supportedVersions: Set<String>

    /**
     * Creates an adapter instance.
     */
    public fun create(): SqlDelightAdapter
}

