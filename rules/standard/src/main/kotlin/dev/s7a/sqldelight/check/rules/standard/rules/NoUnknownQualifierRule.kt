package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts

/**
 * Reports qualified column references whose qualifier is not visible in `FROM`.
 *
 * The rule uses stable source-level facts and intentionally does not perform
 * column resolution. It checks only whether `qualifier.column` can be matched
 * to a table name or table alias visible in the same statement.
 */
public class NoUnknownQualifierRule : Rule {
    override val id: RuleId = RuleId("no-unknown-qualifier")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.facts.statements.forEach { statement ->
            val visibleQualifiers = statement.visibleQualifiers()
            if (visibleQualifiers.isEmpty()) return@forEach

            statement.qualifiedReferences
                .filterNot { reference -> statement.isInsideTableReference(reference.range, context.file.content) }
                .filterNot { reference -> reference.qualifier.lowercase() in visibleQualifiers }
                .forEach { reference ->
                    reporter.report(
                        Diagnostic(
                            ruleId = id,
                            severity = defaultSeverity,
                            message = "Qualifier '${reference.qualifier}' is not declared by a table reference in this statement.",
                            file = context.file,
                            range = reference.range,
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private fun SqlStatementFacts.visibleQualifiers(): Set<String> =
    tableReferences
        .flatMap { reference -> listOfNotNull(reference.alias, reference.name) }
        .map { name -> name.lowercase() }
        .toSet()

private fun SqlStatementFacts.isInsideTableReference(
    range: SourceRange,
    content: String,
): Boolean {
    val start = range.start.toOffsetIn(content)
    val end = range.end.toOffsetIn(content)
    return tableReferences.any { reference ->
        val tableStart = reference.range.start.toOffsetIn(content)
        val tableEnd = reference.range.end.toOffsetIn(content)
        start >= tableStart && end <= tableEnd
    }
}

private fun SourcePosition.toOffsetIn(content: String): Int {
    var line = 1
    var column = 1
    content.forEachIndexed { index, character ->
        if (line == this.line && column == this.column) return index
        if (character == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return content.length
}
