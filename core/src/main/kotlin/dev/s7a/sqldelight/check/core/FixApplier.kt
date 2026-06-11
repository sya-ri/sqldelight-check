package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.TextEdit

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
        val skippedFixes = mutableListOf<SkippedFix>()

        diagnostics.forEach { diagnostic ->
            val fix =
                diagnostic.fixes.firstOrNull { candidate ->
                    candidate.safety == FixSafety.Safe || allowUnsafe
                }
            if (fix == null) {
                skippedFixes += diagnostic.fixes.map { skippedFix ->
                    skippedFix.toSkippedFix(diagnostic, FixSkipReason.Unsafe)
                }
                return@forEach
            }

            val edits =
                fix.edits
                    .mapIndexedNotNull { index, edit -> edit.toOffsetEdit(content, index) }
                    .sortedBy { edit -> edit.startOffset }
            if (edits.size != fix.edits.size) {
                skippedFixes += fix.toSkippedFix(diagnostic, FixSkipReason.InvalidRange)
                return@forEach
            }
            if (edits.hasOverlap()) {
                skippedFixes += fix.toSkippedFix(diagnostic, FixSkipReason.OverlappingEdits)
                return@forEach
            }
            candidates += FixCandidate(diagnostic = diagnostic, fix = fix, edits = edits)
        }

        val selected = mutableListOf<FixCandidate>()
        candidates
            .sortedBy { candidate -> candidate.startOffset }
            .forEach { candidate ->
                if (selected.any { selectedCandidate -> selectedCandidate.overlaps(candidate) }) {
                    skippedFixes += candidate.toSkippedFix(FixSkipReason.OverlappingCandidate)
                } else {
                    selected += candidate
                }
            }

        val builder = StringBuilder(content)
        selected
            .flatMap { candidate -> candidate.edits }
            .sortedWith(
                compareByDescending<OffsetEdit> { edit -> edit.startOffset }
                    .thenByDescending { edit -> edit.index },
            )
            .forEach { edit ->
                builder.replace(edit.startOffset, edit.endOffset, edit.replacement)
            }

        return FixApplicationResult(
            content = builder.toString(),
            appliedFixes = selected.size,
            skippedFixes = skippedFixes.size,
            skippedFixDetails = skippedFixes,
        )
    }
}

private data class FixCandidate(
    val diagnostic: Diagnostic,
    val fix: Fix,
    val edits: List<OffsetEdit>,
) {
    val startOffset: Int = edits.minOf { edit -> edit.startOffset }
    private val endOffset: Int = edits.maxOf { edit -> edit.endOffset }

    fun overlaps(other: FixCandidate): Boolean =
        startOffset < other.endOffset && other.startOffset < endOffset

    fun toSkippedFix(reason: FixSkipReason): SkippedFix = fix.toSkippedFix(diagnostic, reason)
}

private data class OffsetEdit(
    val startOffset: Int,
    val endOffset: Int,
    val replacement: String,
    val index: Int,
)

private fun TextEdit.toOffsetEdit(
    content: String,
    index: Int,
): OffsetEdit? {
    val start = content.offsetAt(range.start) ?: return null
    val end = content.offsetAt(range.end) ?: return null
    if (end < start) return null
    return OffsetEdit(startOffset = start, endOffset = end, replacement = replacement, index = index)
}

private fun List<OffsetEdit>.hasOverlap(): Boolean =
    zipWithNext().any { (left, right) -> right.startOffset < left.endOffset }

private fun Fix.toSkippedFix(
    diagnostic: Diagnostic,
    reason: FixSkipReason,
): SkippedFix =
    SkippedFix(
        ruleId = diagnostic.qualifiedRuleId,
        file = diagnostic.file,
        title = title,
        reason = reason,
    )

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
