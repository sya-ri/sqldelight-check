@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import kotlin.test.assertEquals

internal fun Rule.diagnostics(
    content: String,
    capabilities: Set<DialectCapability> = setOf(DialectCapabilities.Hsql),
    path: String = "src/main/sqldelight/com/example/1.sqm",
    options: Map<String, String> = emptyMap(),
): List<Diagnostic> {
    val targetCapability = this.targetCapability
    if (targetCapability != null && targetCapability !in capabilities) return emptyList()

    val diagnostics = mutableListOf<Diagnostic>()
    run(
        context =
            object : RuleContext {
                override val database: DatabaseContext =
                    DatabaseContext(
                        name = "Database",
                        dialect =
                            SqlDialect(
                                family = DialectFamily.Hsql,
                                displayName = "HSQL",
                                capabilities = capabilities,
                            ),
                    )
                override val file: SourceFile = SourceFile(path, content.trimIndent() + "\n")
                override val options: Map<String, String> = options
                override val facts: SqlFacts = SqlFacts()
            },
        reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic.withRuleSetPrefix("hsql", id) },
    )
    return diagnostics
}

private fun RuleDiagnostic.withRuleSetPrefix(
    prefix: String,
    ruleId: RuleId,
): Diagnostic =
    Diagnostic(
        severity = severity,
        message = message,
        file = file,
        range = range,
        database = database,
        ruleId = QualifiedRuleId(RuleSetId(prefix), ruleId),
        fixes = fixes,
    )

internal fun Rule.assertOne(content: String) {
    assertEquals(1, diagnostics(content).size)
}
