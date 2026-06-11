package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports PostgreSQL DDL patterns that can take strong locks.
 */
public class ExcessiveLocksRule : Rule {
    override val id: RuleId = RuleId("postgres:excessive-locks")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun isApplicable(context: RuleContext): Boolean = context.isPostgreSql()

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("create")) return@forEachIndexed

            val indexToken = tokens.createIndexToken(index) ?: return@forEachIndexed
            val nextToken = tokens.getOrNull(tokens.indexOf(indexToken) + 1)
            if (nextToken?.isKeyword("concurrently") == true) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "Use CREATE INDEX CONCURRENTLY for PostgreSQL indexes that may be built on live tables.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, indexToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun RuleContext.isPostgreSql(): Boolean =
    DialectCapabilities.PostgreSql in database.dialect.capabilities

private fun List<SqlToken>.createIndexToken(createIndex: Int): SqlToken? {
    val first = getOrNull(createIndex + 1) ?: return null
    if (first.isKeyword("index")) return first
    if (!first.isKeyword("unique")) return null
    val second = getOrNull(createIndex + 2) ?: return null
    return if (second.isKeyword("index")) second else null
}

private data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

private fun String.sqlTokens(): Sequence<SqlToken> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlTokens[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlTokens[index] == '"' -> skipQuoted(index, '"')
                    this@sqlTokens[index] == '`' -> skipQuoted(index, '`')
                    this@sqlTokens[index] == '[' -> skipBracketQuoted(index)
                    this@sqlTokens[index].isIdentifierStart() -> {
                        val start = index
                        index++
                        while (index < length && this@sqlTokens[index].isIdentifierPart()) {
                            index++
                        }
                        yield(SqlToken(text = substring(start, index), startOffset = start, endOffset = index))
                        index
                    }
                    else -> index + 1
                }
        }
    }

private fun String.rangeAtOffsets(
    startOffset: Int,
    endOffset: Int,
): SourceRange =
    SourceRange(
        start = positionAt(startOffset),
        end = positionAt(endOffset),
    )

private fun String.positionAt(offset: Int): SourcePosition {
    val boundedOffset = offset.coerceIn(0, length)
    var line = 1
    var lineStart = 0
    var index = 0
    while (index < boundedOffset) {
        if (this[index] == '\n') {
            line++
            lineStart = index + 1
        }
        index++
    }
    return SourcePosition(line = line, column = boundedOffset - lineStart + 1)
}

private fun String.skipLineComment(start: Int): Int {
    val newline = indexOf('\n', startIndex = start)
    return if (newline == -1) length else newline
}

private fun String.skipBlockComment(start: Int): Int {
    val end = indexOf("*/", startIndex = start + 2)
    return if (end == -1) length else end + 2
}

private fun String.skipQuoted(
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == quote) {
            val next = index + 1
            if (next < length && this[next] == quote) {
                index += 2
            } else {
                return next
            }
        } else {
            index++
        }
    }
    return length
}

private fun String.skipBracketQuoted(start: Int): Int {
    val end = indexOf(']', startIndex = start + 1)
    return if (end == -1) length else end + 1
}

private fun Char.isIdentifierStart(): Boolean = isLetter() || this == '_'

private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$'
