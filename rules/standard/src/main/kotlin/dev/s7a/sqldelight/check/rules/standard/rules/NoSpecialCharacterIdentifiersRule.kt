package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports quoted identifiers that contain characters outside portable SQL names.
 *
 * The rule only inspects quoted identifier forms and keeps single-quoted string
 * literals out of scope.
 */
public class NoSpecialCharacterIdentifiersRule : Rule {
    override val id: RuleId = RuleId("no-special-character-identifiers")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.quotedIdentifiers().forEach { identifier ->
            if (identifier.name.all { character -> character == '_' || character.isLetterOrDigit() }) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Identifiers should avoid special characters.",
                    file = context.file,
                    range = content.rangeAtOffsets(identifier.startOffset, identifier.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class QuotedIdentifier(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.quotedIdentifiers(): Sequence<QuotedIdentifier> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@quotedIdentifiers[index] == '\'' -> skipQuoted(index, '\'')
                    this@quotedIdentifiers[index] == '"' -> {
                        val identifier = quotedIdentifier(index, '"', '"')
                        if (identifier != null) yield(identifier)
                        identifier?.endOffset ?: length
                    }
                    this@quotedIdentifiers[index] == '`' -> {
                        val identifier = quotedIdentifier(index, '`', '`')
                        if (identifier != null) yield(identifier)
                        identifier?.endOffset ?: length
                    }
                    this@quotedIdentifiers[index] == '[' -> {
                        val identifier = quotedIdentifier(index, '[', ']')
                        if (identifier != null) yield(identifier)
                        identifier?.endOffset ?: length
                    }
                    else -> index + 1
                }
        }
    }

private fun String.quotedIdentifier(
    start: Int,
    open: Char,
    close: Char,
): QuotedIdentifier? {
    val builder = StringBuilder()
    var index = start + 1
    while (index < length) {
        val current = this[index]
        if (current == close) {
            val next = index + 1
            if (next < length && this[next] == close) {
                builder.append(close)
                index += 2
            } else {
                return QuotedIdentifier(name = builder.toString(), startOffset = start, endOffset = next)
            }
        } else {
            builder.append(current)
            index++
        }
    }
    return null
}
