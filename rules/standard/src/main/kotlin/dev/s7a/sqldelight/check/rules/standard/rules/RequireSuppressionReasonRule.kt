package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports sqldelight-check disable directives that do not include an explanatory reason.
 */
public class RequireSuppressionReasonRule : Rule {
    override val id: RuleId = RuleId("require-suppression-reason")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.lineComments()
            .mapNotNull { comment -> content.disableDirectiveWithoutReason(comment) }
            .forEach { directive ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "sqldelight-check disable directives should include a reason after '--'.",
                        file = context.file,
                        range = content.rangeAtOffsets(directive.startOffset, directive.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class DisableDirectiveWithoutReason(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.disableDirectiveWithoutReason(comment: LineComment): DisableDirectiveWithoutReason? {
    val text = substring(comment.startOffset, comment.endOffset).removeSuffix("\n")
    val body = text.removePrefix("--").trimStart()
    if (!body.startsWith("sqldelight-check-")) return null
    val withoutPrefix = body.removePrefix("sqldelight-check-")
    val command = withoutPrefix.takeWhile { character -> !character.isWhitespace() }
    if (command !in disableDirectiveCommands) return null
    val payload = withoutPrefix.drop(command.length)
    if (payload.hasDirectiveReason()) return null
    return DisableDirectiveWithoutReason(startOffset = comment.startOffset, endOffset = comment.endOffset)
}

private fun String.hasDirectiveReason(): Boolean {
    val delimiter = indexOf(" --")
    if (delimiter == -1) return false
    return drop(delimiter + 3).isNotBlank()
}

private val disableDirectiveCommands = setOf("disable", "disable-next-line", "disable-file")
