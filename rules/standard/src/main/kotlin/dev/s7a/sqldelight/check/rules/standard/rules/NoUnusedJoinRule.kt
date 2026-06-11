package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports JOIN sources that are not referenced by later qualified column reads.
 *
 * The rule is conservative and only reports joined tables with an alias or name
 * that never appears as a qualifier after the JOIN source.
 */
public class NoUnusedJoinRule : Rule {
    override val id: RuleId = RuleId("no-unused-join")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.tableReferences(context.database.dialect.sourcePatterns)
            .filter { reference -> reference.depth == 0 && reference.introducedBy == TableReferenceIntroducer.Join }
            .forEach { reference ->
                val qualifier = reference.alias?.text ?: reference.tableName ?: return@forEach
                if (content.hasQualifiedReferenceAfter(qualifier, reference.sourceEndOffset)) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "JOIN source '$qualifier' is not referenced by later qualified column reads.",
                        file = context.file,
                        range = content.rangeAtOffsets(reference.sourceStartOffset, reference.sourceEndOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.hasQualifiedReferenceAfter(
    qualifier: String,
    offset: Int,
): Boolean {
    val pattern = Regex("""\b${Regex.escape(qualifier)}\s*\.""", RegexOption.IGNORE_CASE)
    val visible = visibleSqlTextAfter(offset)
    return pattern.containsMatchIn(visible)
}

private fun String.visibleSqlTextAfter(offset: Int): String {
    val builder = StringBuilder(length - offset)
    sqlCharacters()
        .dropWhile { character -> character.offset < offset }
        .forEach { character -> builder.append(character.value) }
    return builder.toString()
}
