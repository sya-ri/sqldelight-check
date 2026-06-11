package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports MySQL REPLACE INTO statements.
 *
 * `REPLACE` deletes conflicting rows before inserting, which can trigger
 * cascading side effects that are easy to miss in application code.
 */
public class NoReplaceIntoRule : Rule {
    override val id: RuleId = RuleId("no-replace-into")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens(hashLineComments = true).toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("replace")) return@forEachIndexed

            val intoToken = tokens.getOrNull(index + 1)
            if (intoToken?.isKeyword("into") != true) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message =
                        "Avoid MySQL REPLACE INTO because it can delete and insert rows instead of updating them.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, intoToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
