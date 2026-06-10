package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in HTML reporter.
 */
public class HtmlReporterProvider : ReporterProvider {
    override val id: String = "html"

    override fun create(options: Map<String, String>): Reporter = HtmlReporter
}

private object HtmlReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        output.write(report.toHtml().toByteArray())
    }
}

private fun Report.toHtml(): String =
    buildString {
        appendLine("<!doctype html>")
        appendLine("<html lang=\"en\">")
        appendLine("<head>")
        appendLine("<meta charset=\"utf-8\">")
        appendLine("<title>sqldelight-check report</title>")
        appendLine("<style>")
        appendLine(
            """
            :root {
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
            .severity-info { color: #175cd3; font-weight: 700; }
            """.trimIndent(),
        )
        appendLine("</style>")
        appendLine("</head>")
        appendLine("<body>")
        appendLine("<h1>sqldelight-check report</h1>")
        appendSummary(diagnostics)
        appendLine("<h2>Diagnostics</h2>")
        if (diagnostics.isEmpty()) {
            appendLine("<p>No diagnostics.</p>")
        } else {
            appendLine("<table>")
            appendLine("<thead><tr><th>#</th><th>Severity</th><th>Rule</th><th>Location</th><th>Message</th><th>Fixes</th></tr></thead>")
            appendLine("<tbody>")
            diagnostics.forEachIndexed { index, diagnostic ->
                appendDiagnosticRow(index + 1, diagnostic)
            }
            appendLine("</tbody>")
            appendLine("</table>")
        }
        appendLine("</body>")
        appendLine("</html>")
    }

private fun StringBuilder.appendSummary(diagnostics: List<Diagnostic>) {
    appendLine("<div class=\"summary\" aria-label=\"summary\">")
    appendLine("<div><strong>Total</strong><br>${diagnostics.size}</div>")
    Severity.entries.forEach { severity ->
        val count = diagnostics.count { diagnostic -> diagnostic.severity == severity }
        appendLine("<div><strong>${severity.name.escapeHtml()}</strong><br>$count</div>")
    }
    appendLine("</div>")
}

private fun StringBuilder.appendDiagnosticRow(
    index: Int,
    diagnostic: Diagnostic,
) {
    val severityClass = "severity-${diagnostic.severity.name.lowercase()}"
    append("<tr id=\"diagnostic-$index\">")
    append("<td><a href=\"#diagnostic-$index\">$index</a></td>")
    append("<td class=\"$severityClass\">${diagnostic.severity.name.escapeHtml()}</td>")
    append("<td><code>${(diagnostic.ruleId?.value ?: "-").escapeHtml()}</code></td>")
    append("<td>${diagnostic.locationLabel().escapeHtml()}</td>")
    append("<td>${diagnostic.message.escapeHtml()}</td>")
    append("<td>${diagnostic.fixes.size}</td>")
    appendLine("</tr>")
}

private fun Diagnostic.locationLabel(): String {
    val path = file?.path ?: "-"
    val rangeLabel = range?.toLocationLabel()
    return if (rangeLabel == null) path else "$path:$rangeLabel"
}

private fun SourceRange.toLocationLabel(): String = "${start.line}:${start.column}"

private fun String.escapeHtml(): String =
    buildString {
        this@escapeHtml.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(character)
            }
        }
    }
