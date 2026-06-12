package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.maskSqlCommentsAndQuotedText
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

private val indexPrefixRegexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports indexes on MySQL TEXT and BLOB columns that omit a prefix length.
 *
 * Prefix lengths are required for many MySQL index definitions over large text
 * and binary column types.
 */
public class RequireIndexPrefixLengthRule : Rule {
    override val id: RuleId = RuleId("require-index-prefix-length")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = MySqlDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText(hashLineComments = true)
        val textColumns =
            Regex("""\b([A-Za-z_][A-Za-z0-9_$]*)\s+(?:TINYTEXT|TEXT|MEDIUMTEXT|LONGTEXT|BLOB|MEDIUMBLOB|LONGBLOB)\b""", indexPrefixRegexOptions)
                .findAll(masked)
                .map { match -> match.groupValues[1].lowercase() }
                .toSet()
        if (textColumns.isEmpty()) return

        Regex("""\bCREATE\s+(?:UNIQUE\s+)?INDEX\b(?:(?!;).)*\(([^)]*)\)""", indexPrefixRegexOptions)
            .findAll(masked)
            .forEach { match ->
                val indexedColumns = match.groupValues[1].split(",").map { column -> column.trim() }
                val missingPrefix =
                    indexedColumns.any { column ->
                        val name = column.takeWhile { character -> character.isLetterOrDigit() || character == '_' || character == '$' }
                        name.lowercase() in textColumns && '(' !in column
                    }
                if (!missingPrefix) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Indexes on MySQL TEXT or BLOB columns should specify a prefix length.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                        database = context.database,
                    ),
                )
            }
    }
}
