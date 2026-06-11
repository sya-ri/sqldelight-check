package dev.s7a.sqldelight.check.reporter.text

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

class TextReporterProviderTest {
    @Test
    fun `writes empty report`() {
        val text = TextReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals(
            """
            sqldelight-check diagnostics: 0
            """.trimIndent() + "\n",
            text,
        )
    }

    @Test
    fun `writes one diagnostic with range and fix`() {
        val text = TextReporterProvider().render(Report(diagnostics = listOf(diagnosticWithFix())))

        assertEquals(
            """
            sqldelight-check diagnostics: 1
            Diagnostics:
            - Warning standard:use-is-null at src/commonMain/sqldelight/Player.sq:2:8-2:16
              Use `IS NULL` instead of = NULL.
              fixes:
              - Use IS NULL [safe]
                edit 1: src/commonMain/sqldelight/Player.sq:2:46-2:52 -> "IS NULL"
            """.trimIndent() + "\n",
            text,
        )
    }

    @Test
    fun `writes multiple severities and fileless diagnostic`() {
        val text =
            TextReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:info"),
                                severity = Severity.Info,
                                message = "Informational note",
                                file = SourceFile("src/b.sq", "SELECT 1;"),
                                range = SourceRange(
                                    start = SourcePosition(line = 1, column = 1),
                                    end = SourcePosition(line = 1, column = 7),
                                ),
                            ),
                            diagnostic(
                                qualifiedRuleId = null,
                                severity = Severity.Error,
                                message = "Project configuration is invalid.",
                                file = null,
                                range = null,
                            ),
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:warning"),
                                severity = Severity.Warning,
                                message = "Warning note",
                                file = SourceFile("src/a.sq", "SELECT * FROM player;"),
                                range = null,
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            sqldelight-check diagnostics: 3
            Diagnostics:
            - Error sqldelight-check at <project>
              Project configuration is invalid.
            - Warning standard:warning at src/a.sq
              Warning note
            - Info standard:info at src/b.sq:1:1-1:7
              Informational note
            """.trimIndent() + "\n",
            text,
        )
    }

    @Test
    fun `sorts diagnostics deterministically and preserves stable ties`() {
        val text =
            TextReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:late"),
                                severity = Severity.Warning,
                                message = "Later file",
                                file = SourceFile("src/z.sq", "SELECT 1;"),
                                range = SourceRange(
                                    start = SourcePosition(line = 1, column = 1),
                                    end = SourcePosition(line = 1, column = 7),
                                ),
                            ),
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:tie"),
                                severity = Severity.Warning,
                                message = "Matching diagnostic",
                                fixes =
                                    listOf(
                                        Fix(
                                            title = "First fix",
                                            safety = FixSafety.Unsafe,
                                            edits = emptyList(),
                                        ),
                                    ),
                            ),
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:early"),
                                severity = Severity.Error,
                                message = "Earlier column",
                                range = SourceRange(
                                    start = SourcePosition(line = 2, column = 4),
                                    end = SourcePosition(line = 2, column = 9),
                                ),
                            ),
                            diagnostic(
                                qualifiedRuleId = qualifiedRuleId("standard:tie"),
                                severity = Severity.Warning,
                                message = "Matching diagnostic",
                                fixes =
                                    listOf(
                                        Fix(
                                            title = "Second fix",
                                            safety = FixSafety.Unsafe,
                                            edits = emptyList(),
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            sqldelight-check diagnostics: 4
            Diagnostics:
            - Error standard:early at src/commonMain/sqldelight/Player.sq:2:4-2:9
              Earlier column
            - Warning standard:tie at src/commonMain/sqldelight/Player.sq:2:8-2:16
              Matching diagnostic
              fixes:
              - First fix [unsafe]
            - Warning standard:tie at src/commonMain/sqldelight/Player.sq:2:8-2:16
              Matching diagnostic
              fixes:
              - Second fix [unsafe]
            - Warning standard:late at src/z.sq:1:1-1:7
              Later file
            """.trimIndent() + "\n",
            text,
        )
    }
}

private fun TextReporterProvider.render(report: Report): String {
    val output = ByteArrayOutputStream()
    create().write(report, ByteArrayReportOutput(output))
    return output.toString()
}

private fun diagnosticWithFix(): Diagnostic =
    diagnostic(
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

private fun diagnostic(
    qualifiedRuleId: QualifiedRuleId? = qualifiedRuleId("standard:use-is-null"),
    severity: Severity = Severity.Warning,
    message: String = "Use `IS NULL` instead of = NULL.",
    file: SourceFile? =
        SourceFile(
            path = "src/commonMain/sqldelight/Player.sq",
            content = "selectByDeleted:\nSELECT * FROM player WHERE deleted_at = NULL;\n",
        ),
    range: SourceRange? =
        SourceRange(
            start = SourcePosition(line = 2, column = 8),
            end = SourcePosition(line = 2, column = 16),
        ),
    fixes: List<Fix> = emptyList(),
): Diagnostic =
    Diagnostic(
        ruleId = qualifiedRuleId?.ruleId,
        severity = severity,
        message = message,
        file = file,
        range = range,
        database = null,
        qualifiedRuleId = qualifiedRuleId,
        fixes = fixes,
    )

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
