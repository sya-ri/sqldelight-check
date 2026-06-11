@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonReporterProviderTest {
    @Test
    fun `writes empty report exactly`() {
        val json = JsonReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals(
            """{"summary":{"diagnostics":0,"errors":0,"warnings":0,"infos":0},"diagnostics":[]}""",
            json,
        )
    }

    @Test
    fun `writes diagnostic report exactly`() {
        val json = JsonReporterProvider().render(Report(diagnostics = listOf(diagnostic())))

        assertEquals(
            """{"summary":{"diagnostics":1,"errors":0,"warnings":1,"infos":0},"diagnostics":[{"ruleId":"standard:use-is-null","severity":"warning","message":"Use `IS NULL` instead of = NULL & keep <safe>.","file":"src/commonMain/sqldelight/Player.sq","range":{"start":{"line":2,"column":8},"end":{"line":2,"column":16}},"database":null,"fixes":[{"title":"Use IS NULL","safety":"safe","edits":[{"range":{"start":{"line":2,"column":46},"end":{"line":2,"column":52}},"replacement":"IS NULL"}]}]}]}""",
            json,
        )
    }

    @Test
    fun `writes pretty report exactly`() {
        val json = JsonReporterProvider().render(
            report = Report(diagnostics = emptyList()),
            options = mapOf("prettyPrint" to "true"),
        )

        assertEquals(
            """
            {
                "summary": {
                    "diagnostics": 0,
                    "errors": 0,
                    "warnings": 0,
                    "infos": 0
                },
                "diagnostics": []
            }
            """.trimIndent(),
            json,
        )
    }
}

private fun JsonReporterProvider.render(
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
        ruleId = ruleId,
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
        fixes =
            listOf(
                Fix(
                    title = "Use IS NULL",
                    safety = FixSafety.Safe,
                    edits =
                        listOf(
                            TextEdit(
                                range = SourceRange(
                                    start = SourcePosition(line = 2, column = 46),
                                    end = SourcePosition(line = 2, column = 52),
                                ),
                                replacement = "IS NULL",
                            ),
                        ),
                ),
            ),
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
