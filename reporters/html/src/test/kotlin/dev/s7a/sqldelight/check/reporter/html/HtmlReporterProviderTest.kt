package dev.s7a.sqldelight.check.reporter.html

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
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContains

class HtmlReporterProviderTest {
    @Test
    fun `writes empty report`() {
        val html = HtmlReporterProvider().render(Report(diagnostics = emptyList()))

        assertContains(html, "<h1>sqldelight-check report</h1>")
        assertContains(html, "<p>No diagnostics.</p>")
        assertContains(html, "<div><strong>Total</strong><br>0</div>")
    }

    @Test
    fun `writes navigable diagnostics table`() {
        val html =
            HtmlReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                message = "Use `IS NULL` instead of = NULL & keep <safe>.",
                            ),
                        ),
                ),
            )

        assertContains(html, "<tr id=\"diagnostic-1\">")
        assertContains(html, "<code>standard:use-is-null</code>")
        assertContains(html, "src/commonMain/sqldelight/Player.sq:2:8")
        assertContains(html, "Use `IS NULL` instead of = NULL &amp; keep &lt;safe&gt;.")
        assertContains(html, "<td>1</td>")
    }
}

private fun HtmlReporterProvider.render(report: Report): String {
    val output = ByteArrayOutputStream()
    create().write(report, output)
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
