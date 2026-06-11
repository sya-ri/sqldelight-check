package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports named SQLDelight parameters that are not lower camel case.
 */
public class ParameterNameCaseRule : Rule {
    override val id: RuleId = RuleId("parameter-name-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.namedSqlDelightParameters().forEach { parameter ->
            if (parameter.name.isLowerCamelIdentifier()) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight parameter ':${parameter.name}' should be lower camel case.",
                    file = context.file,
                    range = content.rangeAtOffsets(parameter.nameStartOffset, parameter.nameEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class NamedSqlDelightParameter(
    val name: String,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
)

private fun String.namedSqlDelightParameters(): Sequence<NamedSqlDelightParameter> =
    sequence {
        sqlCharacters().forEach { character ->
            if (character.value != ':') return@forEach
            if (getOrNull(character.offset + 1) == ':') return@forEach
            val start = character.offset + 1
            val name = identifierTokenAt(start) ?: return@forEach
            yield(NamedSqlDelightParameter(name = name.text, nameStartOffset = name.startOffset, nameEndOffset = name.endOffset))
        }
    }
