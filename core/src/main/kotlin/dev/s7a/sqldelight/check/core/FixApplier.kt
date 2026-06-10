package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.TextEdit

/**
 * Result of applying diagnostic fixes to one source file.
 */
public data class FixApplicationResult(
    /** Updated file content after all selected fixes were applied. */
    public val content: String,
    /** Number of fixes applied. */
    public val appliedFixes: Int,
    /** Number of fixes skipped because they were unsafe, invalid, or overlapped a selected fix. */
    public val skippedFixes: Int,
)

/**
 * Applies non-overlapping text edits from diagnostics.
 */
public class FixApplier {
    /**
     * Applies the first allowed fix from each diagnostic to [content].
     */
    public fun apply(
        content: String,
        diagnostics: List<Diagnostic>,
        allowUnsafe: Boolean,
    ): FixApplicationResult {
        val candidates = mutableListOf<FixCandidate>()
        var skippedFixes = 0

        diagnostics.forEach { diagnostic ->
            val fix =
                diagnostic.fixes.firstOrNull { candidate ->
                    candidate.safety == FixSafety.Safe || allowUnsafe
                }
            if (fix == null) {
                skippedFixes += diagnostic.fixes.size
                return@forEach
            }

            val edits =
                fix.edits
                    .mapNotNull { edit -> edit.toOffsetEdit(content) }
                    .sortedBy { edit -> edit.startOffset }
            if (edits.size != fix.edits.size || edits.hasOverlap()) {
                skippedFixes++
                return@forEach
            }
            candidates += FixCandidate(edits)
        }

        val selected = mutableListOf<FixCandidate>()
        candidates
            .sortedBy { candidate -> candidate.startOffset }
            .forEach { candidate ->
                if (selected.any { selectedCandidate -> selectedCandidate.overlaps(candidate) }) {
                    skippedFixes++
                } else {
                    selected += candidate
                }
            }

        val builder = StringBuilder(content)
        selected
            .flatMap { candidate -> candidate.edits }
            .sortedByDescending { edit -> edit.startOffset }
            .forEach { edit ->
                builder.replace(edit.startOffset, edit.endOffset, edit.replacement)
            }

        return FixApplicationResult(
            content = builder.toString(),
            appliedFixes = selected.size,
            skippedFixes = skippedFixes,
        )
    }
}

private data class FixCandidate(
    val edits: List<OffsetEdit>,
) {
    val startOffset: Int = edits.minOf { edit -> edit.startOffset }
    private val endOffset: Int = edits.maxOf { edit -> edit.endOffset }

    fun overlaps(other: FixCandidate): Boolean =
        startOffset < other.endOffset && other.startOffset < endOffset
}

private data class OffsetEdit(
    val startOffset: Int,
    val endOffset: Int,
    val replacement: String,
)

private fun TextEdit.toOffsetEdit(content: String): OffsetEdit? {
    val start = content.offsetAt(range.start) ?: return null
    val end = content.offsetAt(range.end) ?: return null
    if (end < start) return null
    return OffsetEdit(startOffset = start, endOffset = end, replacement = replacement)
}

private fun List<OffsetEdit>.hasOverlap(): Boolean =
    zipWithNext().any { (left, right) -> right.startOffset < left.endOffset }

private fun String.offsetAt(position: SourcePosition): Int? {
    if (position.line < 1 || position.column < 1) return null

    var line = 1
    var lineStart = 0
    while (line < position.line) {
        val newline = indexOf('\n', startIndex = lineStart)
        if (newline == -1) return null
        lineStart = newline + 1
        line++
    }

    val lineEnd =
        indexOf('\n', startIndex = lineStart)
            .takeIf { index -> index != -1 }
            ?: length
    val offset = lineStart + position.column - 1
    if (offset > lineEnd) return null
    return offset
}
