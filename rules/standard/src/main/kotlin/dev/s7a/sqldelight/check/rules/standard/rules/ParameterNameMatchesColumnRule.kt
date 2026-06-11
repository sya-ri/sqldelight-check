package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports simple column predicates whose SQLDelight parameter names do not match the column name.
 */
public class ParameterNameMatchesColumnRule : Rule {
    override val id: RuleId = RuleId("parameter-name-matches-column")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.namedParametersWithColon().forEach { parameter ->
            val equals = content.previousSqlCharacterBefore(parameter.colonOffset)
            if (equals?.value != '=') return@forEach
            val column = content.sqlTokens().takeWhile { token -> token.endOffset <= equals.offset }.lastOrNull() ?: return@forEach
            val expected = column.text.substringAfterLast('.').toLowerCamelFromSnake()
            if (expected == parameter.name) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Parameter ':${parameter.name}' should match column '${column.text}' as ':$expected'.",
                    file = context.file,
                    range = content.rangeAtOffsets(parameter.nameStartOffset, parameter.nameEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class ColumnMatchingParameter(
    val name: String,
    val colonOffset: Int,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
)

private fun String.namedParametersWithColon(): Sequence<ColumnMatchingParameter> =
    sequence {
        sqlCharacters().forEach { character ->
            if (character.value != ':') return@forEach
            if (getOrNull(character.offset + 1) == ':') return@forEach
            val start = character.offset + 1
            if (!getOrNull(start).isParameterNameStart()) return@forEach
            var end = start + 1
            while (getOrNull(end).isParameterNamePart()) {
                end++
            }
            yield(
                ColumnMatchingParameter(
                    name = substring(start, end),
                    colonOffset = character.offset,
                    nameStartOffset = start,
                    nameEndOffset = end,
                ),
            )
        }
    }

private fun Char?.isParameterNameStart(): Boolean = this == '_' || this?.isLetter() == true

private fun Char?.isParameterNamePart(): Boolean = this == '_' || this?.isLetterOrDigit() == true

private fun String.toLowerCamelFromSnake(): String {
    val parts = split('_').filter { part -> part.isNotEmpty() }
    if (parts.isEmpty()) return this
    return parts.first().lowercase() + parts.drop(1).joinToString("") { part -> part.lowercase().replaceFirstChar { it.uppercase() } }
}
