package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

internal fun String.startOwnLineFix(
    offset: Int,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Safe,
        edits = listOf(startOwnLineEdit(offset)),
    )

internal fun String.startOwnLineFix(
    offset: Int,
    title: String,
    indentation: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Safe,
        edits = listOf(startOwnLineEdit(offset, indentation)),
    )

internal fun String.startOwnLineFix(
    offsets: List<Int>,
    title: String,
): Fix =
    Fix(
        title = title,
        safety = FixSafety.Safe,
        edits = offsets.map { offset -> startOwnLineEdit(offset) },
    )

private fun String.startOwnLineEdit(offset: Int): TextEdit {
    return startOwnLineEdit(offset, indentation = "")
}

private fun String.startOwnLineEdit(
    offset: Int,
    indentation: String,
): TextEdit {
    var start = offset
    while (start > 0 && (this[start - 1] == ' ' || this[start - 1] == '\t')) {
        start--
    }
    return TextEdit(range = rangeAtOffsets(start, offset), replacement = "\n$indentation")
}
