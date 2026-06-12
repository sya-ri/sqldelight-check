package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common SQL keywords that are not uppercase.
 */
public class KeywordCaseRule : Rule {
    override val id: RuleId = RuleId("keyword-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = false

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.lowercase() in keywords }
            .filterNot { token -> token.text == token.text.uppercase() }
            .forEach { token ->
                val replacement = token.text.uppercase()
                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "SQL keyword '${token.text}' should be uppercase.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Uppercase keyword",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = replacement)),
                                ),
                            ),
                    ),
                )
            }
    }

    private companion object {
        val keywords =
            setOf(
                "add",
                "alter",
                "and",
                "as",
                "asc",
                "between",
                "by",
                "case",
                "check",
                "column",
                "constraint",
                "create",
                "default",
                "delete",
                "desc",
                "distinct",
                "drop",
                "else",
                "end",
                "exists",
                "foreign",
                "from",
                "group",
                "having",
                "in",
                "index",
                "inner",
                "insert",
                "into",
                "is",
                "join",
                "key",
                "left",
                "like",
                "limit",
                "not",
                "on",
                "or",
                "order",
                "outer",
                "primary",
                "references",
                "right",
                "select",
                "set",
                "table",
                "then",
                "union",
                "unique",
                "update",
                "values",
                "when",
                "where",
            )
    }
}
