package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common SQL data type names that are not uppercase.
 */
public class DataTypeCaseRule : Rule {
    override val id: RuleId = RuleId("data-type-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens
            .withIndex()
            .filter { (_, token) -> token.text.lowercase() in dataTypes }
            .filterNot { (index, token) -> token.isSqlDelightColumnAdapterType(tokens, index, content) }
            .filterNot { (_, token) -> token.text == token.text.uppercase() }
            .forEach { (_, token) ->
                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "SQL data type '${token.text}' should be uppercase.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Uppercase data type",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = token.text.uppercase())),
                                ),
                            ),
                    ),
                )
            }
    }

    private companion object {
        fun SqlToken.isSqlDelightColumnAdapterType(
            tokens: List<SqlToken>,
            index: Int,
            content: String,
        ): Boolean =
            tokens.getOrNull(index - 1)?.isTerm(SqlDialectSourceTerm.As) == true ||
                content.isQualifiedIdentifierSegment(this)

        fun String.isQualifiedIdentifierSegment(token: SqlToken): Boolean =
            previousNonWhitespaceOffset(token.startOffset, '.') != null ||
                nextNonWhitespaceOffset(token.endOffset, '.') != null

        fun String.nextNonWhitespaceOffset(
            offset: Int,
            expected: Char,
        ): Int? {
            var index = offset
            while (index < length && this[index].isWhitespace()) {
                index++
            }
            return if (getOrNull(index) == expected) index else null
        }

        val dataTypes =
            setOf(
                "bigint",
                "blob",
                "bool",
                "boolean",
                "char",
                "clob",
                "decimal",
                "double",
                "float",
                "int",
                "integer",
                "numeric",
                "real",
                "smallint",
                "text",
                "timestamp",
                "varchar",
            )
    }
}
