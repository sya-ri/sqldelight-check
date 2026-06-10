package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common SQL data type names that are not uppercase.
 */
public class DataTypeCaseRule : Rule {
    override val id: RuleId = RuleId("standard:data-type-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.lowercase() in dataTypes }
            .filterNot { token -> token.text == token.text.uppercase() }
            .forEach { token ->
                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    Diagnostic(
                        ruleId = id,
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
