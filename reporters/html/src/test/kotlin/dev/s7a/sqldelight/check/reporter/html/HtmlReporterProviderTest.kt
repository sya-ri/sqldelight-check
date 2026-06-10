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
import kotlin.test.assertEquals

class HtmlReporterProviderTest {
    @Test
    fun `writes empty report`() {
        val html = HtmlReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals(emptyReportHtml, html)
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

        assertEquals(diagnosticsReportHtml, html)
    }
}

private val emptyReportHtml: String =
    """
    <!doctype html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>sqldelight-check report</title>
        <style>:root {
      color-scheme: light dark;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    body { margin: 2rem; line-height: 1.5; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid CanvasText; padding: 0.5rem; text-align: left; vertical-align: top; }
    th { background: color-mix(in srgb, CanvasText 10%, Canvas); }
    code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
    .summary { display: flex; gap: 1rem; flex-wrap: wrap; margin: 1rem 0 2rem; }
    .summary div { border: 1px solid CanvasText; padding: 0.5rem 0.75rem; }
    .severity-error { color: #b42318; font-weight: 700; }
    .severity-warning { color: #a15c07; font-weight: 700; }
    .severity-info { color: #175cd3; font-weight: 700; }</style>
      </head>
      <body>
        <h1>sqldelight-check report</h1>
        <div class="summary" aria-label="summary">
          <div><strong>Total</strong><br>0</div>
          <div><strong>Info</strong><br>0</div>
          <div><strong>Warning</strong><br>0</div>
          <div><strong>Error</strong><br>0</div>
        </div>
        <h2>Diagnostics</h2>
        <p>No diagnostics.</p>
      </body>
    </html>
    """.trimIndent() + "\n"

private val diagnosticsReportHtml: String =
    """
    <!doctype html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>sqldelight-check report</title>
        <style>:root {
      color-scheme: light dark;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    body { margin: 2rem; line-height: 1.5; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid CanvasText; padding: 0.5rem; text-align: left; vertical-align: top; }
    th { background: color-mix(in srgb, CanvasText 10%, Canvas); }
    code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
    .summary { display: flex; gap: 1rem; flex-wrap: wrap; margin: 1rem 0 2rem; }
    .summary div { border: 1px solid CanvasText; padding: 0.5rem 0.75rem; }
    .severity-error { color: #b42318; font-weight: 700; }
    .severity-warning { color: #a15c07; font-weight: 700; }
    .severity-info { color: #175cd3; font-weight: 700; }</style>
      </head>
      <body>
        <h1>sqldelight-check report</h1>
        <div class="summary" aria-label="summary">
          <div><strong>Total</strong><br>1</div>
          <div><strong>Info</strong><br>0</div>
          <div><strong>Warning</strong><br>1</div>
          <div><strong>Error</strong><br>0</div>
        </div>
        <h2>Diagnostics</h2>
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Severity</th>
              <th>Rule</th>
              <th>Location</th>
              <th>Message</th>
              <th>Fixes</th>
            </tr>
          </thead>
          <tbody>
            <tr id="diagnostic-1">
              <td><a href="#diagnostic-1">1</a></td>
              <td class="severity-warning">Warning</td>
              <td><code>standard:use-is-null</code></td>
              <td>src/commonMain/sqldelight/Player.sq:2:8</td>
              <td>Use `IS NULL` instead of = NULL &amp; keep &lt;safe&gt;.</td>
              <td>1</td>
            </tr>
          </tbody>
        </table>
      </body>
    </html>
    """.trimIndent() + "\n"

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
