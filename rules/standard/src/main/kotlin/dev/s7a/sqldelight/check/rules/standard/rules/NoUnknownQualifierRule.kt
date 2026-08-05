package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        val content = context.file.content
        context.facts.statements.forEach { statement ->
            val visibleQualifiers = statement.visibleQualifiers()
            if (visibleQualifiers.isEmpty()) return@forEach

            val tableRanges = statement.tableReferences.map { ref ->
                ref.range.start.toOffsetIn(content) to ref.range.end.toOffsetIn(content)
            }

            statement.qualifiedReferences
                .filterNot { reference ->
                    val start = reference.range.start.toOffsetIn(content)
                    val end = reference.range.end.toOffsetIn(content)
                    tableRanges.any { (ts, te) -> start >= ts && end <= te }
                }
                .filterNot { reference -> reference.qualifier.lowercase() in visibleQualifiers }
                .forEach { reference ->
                    reporter.report(
                        RuleDiagnostic(
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

