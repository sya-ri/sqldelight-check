package dev.s7a.sqldelight.check.reporter.markdown

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
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

class MarkdownReporterProviderTest {
    @Test
    fun `writes empty report`() {
        val markdown = MarkdownReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals(
            """
            # sqldelight-check

            | Total | Errors | Warnings | Infos |
            | ---: | ---: | ---: | ---: |
            | 0 | 0 | 0 | 0 |

            ## Diagnostics

            No diagnostics.
            """.trimIndent() + "\n",
            markdown,
        )
    }

    @Test
    fun `writes escaped diagnostics table`() {
        val markdown =
            MarkdownReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                message = "Avoid pipe | and newline\ninside messages.",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            # sqldelight-check

            | Total | Errors | Warnings | Infos |
            | ---: | ---: | ---: | ---: |
            | 1 | 0 | 1 | 0 |

            ## Diagnostics

            | Severity | Rule | Location | Message | Fixes |
            | --- | --- | --- | --- | ---: |
            | Warning | `standard:use-is-null` | src/commonMain/sqldelight/Player.sq:2:8 | Avoid pipe \| and newline<br>inside messages. | 1 |
            """.trimIndent() + "\n",
            markdown,
        )
    }
}

private fun MarkdownReporterProvider.render(report: Report): String {
    val output = ByteArrayOutputStream()
    create().write(report, ByteArrayReportOutput(output))
    return output.toString()
}

private fun diagnostic(message: String): Diagnostic =
    Diagnostic(
        ruleId = RuleId("standard:use-is-null"),
        severity = Severity.Warning,
        message = message,
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

private class ByteArrayReportOutput(
    private val output: ByteArrayOutputStream,
) : ReportOutput {
    override fun file(): OutputStream = output

    override fun file(path: String): OutputStream = output
}
