package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

internal fun String.insertTokenFix(
    offset: Int,
    replacement: String,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits = listOf(TextEdit(range = rangeAtOffsets(offset, offset), replacement = replacement)),
    )

internal fun String.deleteTokenFix(
    startOffset: Int,
    endOffset: Int,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits = listOf(TextEdit(range = rangeAtOffsets(startOffset, endOffset), replacement = "")),
    )

internal fun String.replaceTokenFix(
    startOffset: Int,
    endOffset: Int,
    replacement: String,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits = listOf(TextEdit(range = rangeAtOffsets(startOffset, endOffset), replacement = replacement)),
    )

internal fun String.deletePairFix(
    firstStartOffset: Int,
    firstEndOffset: Int,
    secondStartOffset: Int,
    secondEndOffset: Int,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits =
            listOf(
                TextEdit(range = rangeAtOffsets(firstStartOffset, firstEndOffset), replacement = ""),
                TextEdit(range = rangeAtOffsets(secondStartOffset, secondEndOffset), replacement = ""),
            ),
    )

internal fun String.inlineWhitespaceEndAfter(offset: Int): Int {
    var index = offset
    while (index < length && (this[index] == ' ' || this[index] == '\t')) {
        index++
    }
    return index
}

internal fun String.inlineWhitespaceStartBefore(offset: Int): Int {
    var index = offset
    while (index > 0 && (this[index - 1] == ' ' || this[index - 1] == '\t')) {
        index--
    }
    return index
}

internal fun String.deleteAliasFix(
    aliasStartOffset: Int,
    aliasEndOffset: Int,
    title: String,
): Fix {
    val previous = sqlTokens().lastOrNull { token -> token.endOffset <= aliasStartOffset }
    val startOffset =
        if (previous?.isTerm(dev.s7a.sqldelight.check.api.SqlDialectSourceTerm.As) == true) {
            inlineWhitespaceStartBefore(previous.startOffset)
        } else {
            inlineWhitespaceStartBefore(aliasStartOffset)
        }
    return deleteTokenFix(startOffset, aliasEndOffset, title)
}

internal fun String.moveLeadingTokenToPreviousLineFix(
    startOffset: Int,
    endOffset: Int,
    title: String,
    prefix: String = "",
): Fix {
    val lines = linesWithRanges()
    val line = lines.lineContaining(startOffset)
    val previousLine = line?.let { current -> lines.lastOrNull { candidate -> candidate.number == current.number - 1 } }
    val tokenText = substring(startOffset, endOffset)
    return Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits =
            listOfNotNull(
                previousLine?.let { TextEdit(range = rangeAtOffsets(it.endOffset, it.endOffset), replacement = "$prefix$tokenText") },
                TextEdit(range = rangeAtOffsets(startOffset, endOffset), replacement = ""),
            ),
    )
}

internal fun String.moveTokenAfterFix(
    sourceStartOffset: Int,
    sourceEndOffset: Int,
    targetEndOffset: Int,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Unsafe,
        edits =
            listOf(
                TextEdit(range = rangeAtOffsets(targetEndOffset, targetEndOffset), replacement = " ${substring(sourceStartOffset, sourceEndOffset)}"),
                TextEdit(range = rangeAtOffsets(sourceStartOffset, sourceEndOffset), replacement = ""),
            ),
    )
