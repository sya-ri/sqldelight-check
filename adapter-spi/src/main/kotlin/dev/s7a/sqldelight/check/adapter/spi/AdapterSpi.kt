package dev.s7a.sqldelight.check.adapter.spi

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourceFile
import java.io.File

/**
 * Input required by a SQLDelight-version adapter to analyze one database.
 */
public data class AnalysisInput(
    /** Database metadata resolved by the Gradle plugin. */
    public val database: DatabaseContext,
    /** Source files that belong to the database. */
    public val files: List<SourceFile>,
    /** SQLDelight package name configured for the database. */
    public val packageName: String? = null,
    /** Source folders that SQLDelight should parse for this database. */
    public val sourceFolders: List<File> = emptyList(),
    /** Dependency source folders from other SQLDelight databases. */
    public val dependencyFolders: List<File> = emptyList(),
    /** SQLDelight compiler/runtime classpath used by the original SQLDelight Gradle task. */
    public val compilerClasspath: List<File> = emptyList(),
    /** Runtime classpath used for loading the configured SQLDelight dialect. */
    public val dialectClasspath: List<File> = emptyList(),
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
     * Returns whether this provider can analyze a project that uses [version].
     *
     * Provider implementations may override this when they support a compatibility range. The default keeps exact
     * matching for simple adapters.
     */
    public fun supports(version: String): Boolean = version in supportedVersions

    /**
     * Creates an adapter instance.
     */
    public fun create(): SqlDelightAdapter
}
