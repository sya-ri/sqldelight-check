package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        content.mappedTypeNames(context.database.dialect.sourcePatterns).forEach { type ->
            if (type.text.substringAfterLast('.').firstOrNull()?.isUpperCase() == true) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight mapped type '${type.text}' should use upper camel case.",
                    file = context.file,
                    range = content.rangeAtOffsets(type.startOffset, type.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
