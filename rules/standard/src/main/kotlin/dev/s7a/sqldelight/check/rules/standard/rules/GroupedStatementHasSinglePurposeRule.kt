package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLDelight grouped statements that mix read and write statements.
 */
public class GroupedStatementHasSinglePurposeRule : Rule {
    override val id: RuleId = RuleId("grouped-statement-has-single-purpose")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.groupedStatementRanges().forEach { group ->
            val verbs =
                content
                    .sqlTokens()
                    .filter { token -> token.startOffset in group.bodyStartOffset until group.bodyEndOffset }
                    .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == group.bodyDepth }
                    .filter { token ->
                        token.matches(
                            context.database.dialect.sourcePatterns,
                            SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart,
                        )
                    }
                    .toSet()
            if (verbs.none { token -> token.isTerm(SqlDialectSourceTerm.Select) }) return@forEach
            if (verbs.none { token -> writeStatementTerms.any { term -> token.isTerm(term) } }) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Grouped SQLDelight statements should not mix read and write statements.",
                    file = context.file,
                    range = content.rangeAtOffsets(group.nameStartOffset, group.nameEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class GroupedStatementRange(
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val bodyStartOffset: Int,
    val bodyEndOffset: Int,
    val bodyDepth: Int,
)

private val writeStatementTerms = setOf(SqlDialectSourceTerm.Delete, SqlDialectSourceTerm.Insert, SqlDialectSourceTerm.Update)

private fun String.groupedStatementRanges(): Sequence<GroupedStatementRange> =
    sequence {
        linesWithRanges().forEach { line ->
            val first = line.firstNonWhitespaceOffset ?: return@forEach
            val name = identifierTokenAt(first) ?: return@forEach
            val openOffset = nextNonHorizontalWhitespaceOffset(name.endOffset) ?: return@forEach
            if (getOrNull(openOffset) != '{') return@forEach
            val closeOffset = matchingClosingBraceOffset(openOffset) ?: return@forEach
            yield(
                GroupedStatementRange(
                    nameStartOffset = name.startOffset,
                    nameEndOffset = name.endOffset,
                    bodyStartOffset = openOffset + 1,
                    bodyEndOffset = closeOffset,
                    bodyDepth = sqlParenthesisDepthAt(openOffset),
                ),
            )
        }
    }
