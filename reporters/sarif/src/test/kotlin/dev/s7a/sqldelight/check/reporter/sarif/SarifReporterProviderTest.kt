@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class SarifReporterProviderTest {
    @Test
    fun `writes empty report exactly`() {
        val sarif = SarifReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals(
            """{"version":"2.1.0","${'$'}schema":"https://json.schemastore.org/sarif-2.1.0.json","runs":[{"tool":{"driver":{"name":"sqldelight-check","semanticVersion":"0.1.0","rules":[]}},"results":[]}]}""",
            sarif,
        )
    }

    @Test
    fun `writes diagnostic report exactly`() {
        val sarif = SarifReporterProvider().render(Report(diagnostics = listOf(diagnostic())))

        assertEquals(
            """{"version":"2.1.0","${'$'}schema":"https://json.schemastore.org/sarif-2.1.0.json","runs":[{"tool":{"driver":{"name":"sqldelight-check","semanticVersion":"0.1.0","rules":[{"id":"standard:use-is-null"}]}},"results":[{"ruleId":"standard:use-is-null","level":"warning","message":{"text":"Use `IS NULL` instead of = NULL & keep <safe>."},"locations":[{"physicalLocation":{"artifactLocation":{"uri":"src/commonMain/sqldelight/Player.sq"},"region":{"startLine":2,"startColumn":8,"endLine":2,"endColumn":16}}}]}]}]}""",
            sarif,
        )
    }

    @Test
    fun `writes pretty report exactly`() {
        val sarif = SarifReporterProvider().render(
            report = Report(diagnostics = emptyList()),
            options = mapOf("prettyPrint" to "true"),
        )

        assertEquals(
            """
            {
                "version": "2.1.0",
                "${'$'}schema": "https://json.schemastore.org/sarif-2.1.0.json",
                "runs": [
                    {
                        "tool": {
                            "driver": {
                                "name": "sqldelight-check",
                                "semanticVersion": "0.1.0",
                                "rules": []
                            }
                        },
                        "results": []
                    }
                ]
            }
            """.trimIndent(),
            sarif,
        )
    }
}

private fun SarifReporterProvider.render(
    report: Report,
    options: Map<String, String> = emptyMap(),
): String {
    val output = ByteArrayOutputStream()
    create(options).write(report, ByteArrayReportOutput(output))
    return output.toString()
}

private fun diagnostic(): Diagnostic {
    val ruleId = qualifiedRuleId("standard:use-is-null")
    return Diagnostic(
        severity = Severity.Warning,
        message = "Use `IS NULL` instead of = NULL & keep <safe>.",
        file = SourceFile(
            path = "src/commonMain/sqldelight/Player.sq",
            content = "selectByDeleted:\nSELECT * FROM player WHERE deleted_at = NULL;\n",
        ),
        range = SourceRange(
            start = SourcePosition(line = 2, column = 8),
            end = SourcePosition(line = 2, column = 16),
        ),
        database = null,
        ruleId = ruleId,
    )
}

private class ByteArrayReportOutput(
    private val output: ByteArrayOutputStream,
) : ReportOutput {
    override fun file(): OutputStream = output

    override fun file(path: String): OutputStream = output
}

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
