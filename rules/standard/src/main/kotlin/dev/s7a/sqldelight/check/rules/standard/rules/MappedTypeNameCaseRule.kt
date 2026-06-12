package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports SQLDelight mapped type names that do not look like Kotlin type names.
 */
public class MappedTypeNameCaseRule : Rule {
    override val id: RuleId = RuleId("mapped-type-name-case")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.mappedTypeTokens(context.database.dialect.sourcePatterns).forEach { token ->
            if (token.text.substringAfterLast('.').firstOrNull()?.isUpperCase() == true) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight mapped type '${token.text}' should use upper camel case.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.mappedTypeTokens(sourcePatterns: SqlDialectSourcePatterns): Sequence<SqlToken> =
    sequence {
        val tokens = sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.As)) return@forEachIndexed
            val previous = tokens.getOrNull(index - 1) ?: return@forEachIndexed
            val next = tokens.getOrNull(index + 1) ?: return@forEachIndexed
            if (!previous.matches(sourcePatterns, SqlDialectSourcePatternRole.SqlDelightMappableStorageTypeName)) return@forEachIndexed
            if (next.matches(sourcePatterns, SqlDialectSourcePatternRole.ColumnConstraintStart)) return@forEachIndexed
            yield(next)
        }
    }
