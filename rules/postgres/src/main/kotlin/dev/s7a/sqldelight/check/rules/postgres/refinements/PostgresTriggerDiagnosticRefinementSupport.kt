package dev.s7a.sqldelight.check.rules.postgres.refinements

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.sqlTokens

internal fun refineDiagnostic(
    context: RuleContext,
    diagnostic: Diagnostic,
    shouldSuppress: String.(Diagnostic) -> Boolean,
): Diagnostic? =
    if (context.file.content.shouldSuppress(diagnostic)) {
        null
    } else {
        diagnostic
    }

internal fun String.isTriggerUpdateEvent(diagnostic: Diagnostic): Boolean {
    val range = diagnostic.range ?: return false
    val offset = offsetAt(range.start)
    val tokens = sqlTokens().toList()
    val updateIndex =
        tokens.indexOfFirst { token ->
            token.startOffset == offset && token.isKeyword("UPDATE")
        }
    if (updateIndex == -1) return false

    val statementStartIndex = tokens.indexOfLastBefore(updateIndex) { token -> token.text == ";" } + 1
    val statementEndIndex = tokens.indexOfFirstAfter(updateIndex) { token -> token.text == ";" }.let { index ->
        if (index == -1) tokens.size else index
    }
    val statementTokens = tokens.subList(statementStartIndex, statementEndIndex)
    val updateInStatement = updateIndex - statementStartIndex
    return statementTokens.isCreateTriggerPrefixBefore(updateInStatement) &&
        statementTokens.drop(updateInStatement + 1).any { token -> token.isKeyword("ON") }
}

internal fun String.isTriggerBodyBoundaryTerminatorDiagnostic(diagnostic: Diagnostic): Boolean {
    val range = diagnostic.range ?: return false
    val offset = offsetAt(range.start)
    val tokens = sqlTokens().toList()
    val previousTokenIndex = tokens.indexOfLast { token -> token.startOffset <= offset }
    if (previousTokenIndex == -1) return false

    val statementStartIndex = tokens.indexOfLastBefore(previousTokenIndex) { token -> token.text == ";" } + 1
    val prefix = tokens.subList(statementStartIndex, previousTokenIndex + 1)
    if (!prefix.isCreateTriggerPrefix()) return false

    if (tokens[previousTokenIndex].isKeyword("BEGIN")) return true

    return tokens
        .drop(previousTokenIndex + 1)
        .takeWhile { token -> token.text != ";" }
        .any { token ->
            token.isKeyword("BEGIN") ||
                token.isKeyword("EXECUTE") ||
                token.isKeyword("ON")
        }
}

internal fun String.isTriggerClauseIndentationDiagnostic(diagnostic: Diagnostic): Boolean {
    val range = diagnostic.range ?: return false
    val lineText = lineAt(range.start.line) ?: return false
    if (!lineText.trimStart().isTriggerClauseLine()) return false

    val lineStartOffset = offsetAt(SourcePosition(line = range.start.line, column = 1))
    val statementStartOffset = lastIndexOf(';', startIndex = (lineStartOffset - 1).coerceAtLeast(0)).let { offset ->
        if (offset == -1) 0 else offset + 1
    }
    val prefixTokens = substring(statementStartOffset, lineStartOffset).sqlTokens().toList()
    return prefixTokens.isCreateTriggerPrefix()
}

private fun List<SqlToken>.isCreateTriggerPrefixBefore(endExclusive: Int): Boolean {
    if (take(endExclusive).any { token -> token.isKeyword("BEGIN") }) return false
    if (!take(endExclusive).isCreateTriggerPrefix()) return false
    val prefix = take(endExclusive)
    return prefix.any { token -> token.isKeyword("BEFORE") || token.isKeyword("AFTER") } ||
        prefix.zipWithNext().any { (first, second) ->
            first.isKeyword("INSTEAD") && second.isKeyword("OF")
        }
}

private fun List<SqlToken>.isCreateTriggerPrefix(): Boolean {
    if (size < 2) return false
    var index = 0
    if (!this[index].isKeyword("CREATE")) return false
    index++
    if (getOrNull(index)?.isKeyword("OR") == true && getOrNull(index + 1)?.isKeyword("REPLACE") == true) {
        index += 2
    }
    if (getOrNull(index)?.isKeyword("CONSTRAINT") == true) {
        index++
    }
    return getOrNull(index)?.isKeyword("TRIGGER") == true
}

private fun String.isTriggerClauseLine(): Boolean =
    startsWith("BEFORE ", ignoreCase = true) ||
        startsWith("AFTER ", ignoreCase = true) ||
        startsWith("INSTEAD OF ", ignoreCase = true) ||
        equals("FOR EACH ROW", ignoreCase = true) ||
        equals("FOR EACH STATEMENT", ignoreCase = true) ||
        startsWith("EXECUTE FUNCTION ", ignoreCase = true) ||
        startsWith("EXECUTE PROCEDURE ", ignoreCase = true)

private inline fun List<SqlToken>.indexOfLastBefore(
    endExclusive: Int,
    predicate: (SqlToken) -> Boolean,
): Int {
    var index = endExclusive - 1
    while (index >= 0) {
        if (predicate(this[index])) return index
        index--
    }
    return -1
}

private inline fun List<SqlToken>.indexOfFirstAfter(
    startExclusive: Int,
    predicate: (SqlToken) -> Boolean,
): Int {
    var index = startExclusive + 1
    while (index < size) {
        if (predicate(this[index])) return index
        index++
    }
    return -1
}

private fun String.lineAt(line: Int): String? =
    lineSequence().drop(line - 1).firstOrNull()

private fun String.offsetAt(position: SourcePosition): Int {
    var line = 1
    var column = 1
    for (index in indices) {
        if (line == position.line && column == position.column) return index
        if (this[index] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return length
}
