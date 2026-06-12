package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.mysql.IntegerDisplayWidthType
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.SqlTokenMatch
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports deprecated MySQL integer display width declarations.
 *
 * Display widths no longer affect integer storage and create unnecessary schema
 * noise in modern MySQL versions.
 */
public class NoDisplayWidthIntegerRule : Rule {
    override val id: RuleId = RuleId("no-display-width-integer")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = MySqlDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid deprecated MySQL integer display widths.",
            hashLineComments = true,
        ) { statement ->
            statement.findDisplayWidthInteger(content, context.database.dialect.sourcePatterns)
        }
    }
}

private fun List<SqlToken>.findDisplayWidthInteger(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): SqlTokenMatch? =
    indices.firstNotNullOfOrNull { index ->
        val type = get(index)
        val width = getOrNull(index + 1) ?: return@firstNotNullOfOrNull null
        if (!sourcePatterns.matches(IntegerDisplayWidthType, listOf(type.normalizedText))) {
            return@firstNotNullOfOrNull null
        }
        if (!width.text.all(Char::isDigit)) return@firstNotNullOfOrNull null
        if (!content.hasParenthesizedGap(type.endOffset, width.startOffset, width.endOffset)) {
            return@firstNotNullOfOrNull null
        }
        SqlTokenMatch(type, width)
    }

private fun String.hasParenthesizedGap(
    typeEndOffset: Int,
    widthStartOffset: Int,
    widthEndOffset: Int,
): Boolean =
    substring(typeEndOffset, widthStartOffset).contains('(') &&
        substring(widthEndOffset).dropWhile(Char::isWhitespace).firstOrNull() == ')'
