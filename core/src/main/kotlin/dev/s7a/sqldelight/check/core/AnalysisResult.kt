package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Stable analysis result produced before rule execution.
 */
public data class AnalysisResult(
    /** Files accepted for downstream rule and formatter execution. */
    public val files: List<SourceFile>,
    /** Diagnostics emitted by SQLDelight or core validation. */
    public val diagnostics: List<Diagnostic>,
)
