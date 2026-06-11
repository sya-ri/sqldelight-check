package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

internal fun Rule.diagnostics(
    content: String,
    capabilities: Set<DialectCapability> = setOf(DialectCapabilities.PostgreSql),
    options: Map<String, String> = emptyMap(),
): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    run(
        context =
            object : RuleContext {
                override val database: DatabaseContext =
                    DatabaseContext(
                        name = "Database",
                        dialect =
                            SqlDialect(
                                family = DialectFamily.PostgreSql,
                                displayName = "PostgreSQL",
                                capabilities = capabilities,
                            ),
                    )
                override val file: SourceFile =
                    SourceFile(
                        path = "src/main/sqldelight/com/example/1.sqm",
                        content = content.trimIndent() + "\n",
                    )
                override val options: Map<String, String> = options
            },
        reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic },
    )
    return diagnostics
}
