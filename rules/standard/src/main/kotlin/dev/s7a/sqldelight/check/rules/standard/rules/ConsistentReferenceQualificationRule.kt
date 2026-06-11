package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlResultColumnFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts

/**
 * Reports single-table SELECT lists that mix qualified and unqualified references.
 *
 * The rule deliberately checks only simple result-column references. Expressions,
 * wildcards, and multi-table statements require stronger name resolution before
 * a style diagnostic can be issued without noisy false positives.
 */
public class ConsistentReferenceQualificationRule : Rule {
    override val id: RuleId = RuleId("standard:consistent-reference-qualification")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.facts.statements.forEach { statement ->
            val select = statement.select ?: return@forEach
            val qualifier = statement.singleVisibleQualifier() ?: return@forEach
            val styles =
                select.resultColumns
                    .mapNotNull { column -> column.referenceQualificationStyle(context.file.content, qualifier) }
                    .toSet()
            if (styles.size < 2) return@forEach

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "Single-table SELECT result columns should use qualification consistently.",
                    file = context.file,
                    range = select.selectListRange,
                    database = context.database,
                ),
            )
        }
    }
}

private enum class QualificationStyle {
    Qualified,
    Unqualified,
}

private fun SqlStatementFacts.singleVisibleQualifier(): String? {
    if (tableReferences.size != 1) return null
    val table = tableReferences.single()
    return table.alias ?: table.name
}

private fun SqlResultColumnFacts.referenceQualificationStyle(
    content: String,
    visibleQualifier: String,
): QualificationStyle? {
    if (wildcard) return null
    val expression = expressionText(content)
    if (simpleIdentifierRegex.matches(expression)) return QualificationStyle.Unqualified

    val match = qualifiedIdentifierRegex.matchEntire(expression) ?: return null
    return if (match.groupValues[1].equals(visibleQualifier, ignoreCase = true)) {
        QualificationStyle.Qualified
    } else {
        null
    }
}

private fun SqlResultColumnFacts.expressionText(content: String): String {
    var text = content.substring(range.start.toOffsetIn(content), range.end.toOffsetIn(content)).trim()
    val aliasText = alias ?: return text
    text = text.replace(Regex("""(?i)\s+as\s+${Regex.escape(aliasText)}\s*$"""), "")
    text = text.replace(Regex("""\s+${Regex.escape(aliasText)}\s*$"""), "")
    return text.trim()
}

private fun SourcePosition.toOffsetIn(content: String): Int {
    var line = 1
    var column = 1
    content.forEachIndexed { index, character ->
        if (line == this.line && column == this.column) return index
        if (character == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return content.length
}

private val simpleIdentifierRegex = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

private val qualifiedIdentifierRegex = Regex("""([A-Za-z_][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)""")
