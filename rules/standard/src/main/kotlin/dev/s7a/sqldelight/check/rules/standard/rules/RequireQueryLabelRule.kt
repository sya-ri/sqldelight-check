package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports executable SQLDelight statements in `.sq` files that are not introduced by a query label.
 */
public class RequireQueryLabelRule : Rule {
    override val id: RuleId = RuleId("require-query-label")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val lines = content.linesWithRanges()
        val parenthesisDepths = content.computeParenthesisDepths()
        lines.forEachIndexed { index, line ->
            val first = line.firstNonWhitespaceOffset ?: return@forEachIndexed
            val token = content.identifierTokenAt(first) ?: return@forEachIndexed
            if (!token.matches(context.database.dialect.sourcePatterns, SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart)) {
                return@forEachIndexed
            }
            if (parenthesisDepths[token.startOffset] > 0) return@forEachIndexed
            val previousLine = lines.previousSignificantLine(index)
            if (previousLine?.isSqlDelightLabelOrGroupStart() == true) return@forEachIndexed
            if (!content.isExecutableStatementStart(token.startOffset)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Executable statements in .sq files should be introduced by a SQLDelight query label.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.isExecutableStatementStart(offset: Int): Boolean {
    val previous = previousSqlCharacterBefore(offset) ?: return true
    return previous.value == ';'
}

private fun List<LineInfo>.previousSignificantLine(index: Int): LineInfo? =
    asSequence()
        .take(index)
        .filter { line -> line.text.isNotBlank() }
        .filterNot { line -> line.text.trimStart().startsWith("--") }
        .lastOrNull()

private fun LineInfo.isSqlDelightLabelOrGroupStart(): Boolean {
    val trimmed = text.trim()
    if (trimmed.endsWith("{")) return true
    val colon = trimmed.indexOf(':')
    return colon > 0 && trimmed.indexOf("::") == -1 && trimmed.substring(0, colon).all { it == '_' || it.isLetterOrDigit() }
}
