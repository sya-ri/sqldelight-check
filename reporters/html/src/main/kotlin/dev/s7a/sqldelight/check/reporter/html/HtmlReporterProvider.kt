package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream
import kotlinx.html.BODY
import kotlinx.html.TBODY
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe

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
    "<!doctype html>\n" +
        createHTML().html {
            lang = "en"
            head {
                meta(charset = "utf-8")
                title { +"sqldelight-check report" }
                style {
                    unsafe {
                        +"""
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
                        """.trimIndent()
                    }
                }
            }
            body {
                h1 { +"sqldelight-check report" }
                summary(diagnostics)
                h2 { +"Diagnostics" }
                if (diagnostics.isEmpty()) {
                    p { +"No diagnostics." }
                } else {
                    table {
                        thead {
                            tr {
                                th { +"#" }
                                th { +"Severity" }
                                th { +"Rule" }
                                th { +"Location" }
                                th { +"Message" }
                                th { +"Fixes" }
                            }
                        }
                        tbody {
                            diagnostics.forEachIndexed { index, diagnostic ->
                                diagnosticRow(index + 1, diagnostic)
                            }
                        }
                    }
                }
            }
        }

private fun BODY.summary(diagnostics: List<Diagnostic>) {
    div("summary") {
        attributes["aria-label"] = "summary"
        div {
            strong { +"Total" }
            br {}
            +"${diagnostics.size}"
        }
        Severity.entries.forEach { severity ->
            div {
                strong { +severity.name }
                br {}
                +"${diagnostics.count { diagnostic -> diagnostic.severity == severity }}"
            }
        }
    }
}

private fun TBODY.diagnosticRow(
    index: Int,
    diagnostic: Diagnostic,
) {
    tr {
        id = "diagnostic-$index"
        td {
            a(href = "#diagnostic-$index") { +"$index" }
        }
        td("severity-${diagnostic.severity.name.lowercase()}") { +diagnostic.severity.name }
        td {
            code { +(diagnostic.ruleId?.value ?: "-") }
        }
        td { +diagnostic.locationLabel() }
        td { +diagnostic.message }
        td { +"${diagnostic.fixes.size}" }
    }
}

private fun Diagnostic.locationLabel(): String {
    val path = file?.path ?: "-"
    val rangeLabel = range?.toLocationLabel()
    return if (rangeLabel == null) path else "$path:$rangeLabel"
}

private fun SourceRange.toLocationLabel(): String = "${start.line}:${start.column}"
