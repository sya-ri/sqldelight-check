package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

/**
 * Provider for the built-in HTML reporter.
 */
public class HtmlReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("html")

    override fun create(options: Map<String, String>): Reporter = HtmlReporter
}

private object HtmlReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.toHtml().toByteArray())
        }
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
                          color-scheme: light;
                          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                          background: #f6f8fa;
                          color: #1f2328;
                        }
                        body {
                          margin: 0;
                          line-height: 1.5;
                          background: #f6f8fa;
                          color: #1f2328;
                        }
                        main {
                          max-width: 72rem;
                          margin: 0 auto;
                          padding: 2rem;
                        }
                        code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
                        .summary { display: flex; gap: 1rem; flex-wrap: wrap; margin: 1rem 0 2rem; }
                        .summary div {
                          border: 1px solid #d0d7de;
                          border-radius: 0.5rem;
                          padding: 0.5rem 0.75rem;
                          min-width: 5rem;
                          background: #ffffff;
                        }
                        .diagnostics { display: grid; gap: 1rem; }
                        .diagnostic-card {
                          border: 1px solid #d0d7de;
                          border-radius: 0.5rem;
                          padding: 1rem;
                          background: #ffffff;
                          box-shadow: 0 1px 2px rgb(31 35 40 / 0.08);
                        }
                        .diagnostic-card h3 { margin: 0 0 0.75rem; }
                        .meta { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
                        .meta span {
                          border: 1px solid #d0d7de;
                          border-radius: 999px;
                          padding: 0.125rem 0.5rem;
                          font-size: 0.875rem;
                          background: #f6f8fa;
                        }
                        .message { margin: 0.75rem 0; }
                        .fix-preview {
                          display: grid;
                          grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
                          gap: 1rem;
                          margin-top: 1rem;
                        }
                        .code-panel strong { display: block; margin-bottom: 0.375rem; }
                        .code-frame {
                          overflow-x: auto;
                          border: 1px solid #d0d7de;
                          border-radius: 0.5rem;
                          background: #f6f8fa;
                          padding: 0.75rem 0;
                        }
                        .code-line {
                          display: grid;
                          grid-template-columns: 4rem 1fr;
                          gap: 1rem;
                          min-width: max-content;
                          padding: 0 0.75rem;
                          margin: 0;
                          font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
                          white-space: pre;
                        }
                        .line-number { color: #6e7781; text-align: right; user-select: none; }
                        .marked { background: #fff1a7; border-bottom: 2px solid #f79009; }
                        .severity-error { color: #b42318; font-weight: 700; }
                        .severity-warning { color: #a15c07; font-weight: 700; }
                        .severity-info { color: #175cd3; font-weight: 700; }
                        """.trimIndent()
                    }
                }
            }
            body {
                main {
                    h1 { +"sqldelight-check report" }
                    summary(diagnostics)
                    h2 { +"Diagnostics" }
                    if (diagnostics.isEmpty()) {
                        p { +"No diagnostics." }
                    } else {
                        div("diagnostics") {
                            diagnostics.forEachIndexed { index, diagnostic ->
                                diagnosticCard(index + 1, diagnostic)
                            }
                        }
                    }
                }
            }
        }

private fun FlowContent.summary(diagnostics: List<Diagnostic>) {
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

private fun FlowContent.diagnosticCard(
    index: Int,
    diagnostic: Diagnostic,
) {
    div("diagnostic-card") {
        id = "diagnostic-$index"
        h3 {
            a(href = "#diagnostic-$index") { +"#$index" }
            +" "
            code { +diagnostic.ruleId.value }
        }
        div("meta") {
            span("severity-${diagnostic.severity.name.lowercase()}") { +diagnostic.severity.name }
            span { +diagnostic.locationLabel() }
            span { +"${diagnostic.fixes.size} fix(es)" }
        }
        p("message") { +diagnostic.message }
        val currentExcerpt = diagnostic.codeExcerpt()
        val fixedExcerpt = diagnostic.fixedExcerpt()
        when {
            currentExcerpt != null && fixedExcerpt != null ->
                div("fix-preview") {
                    codePanel("Current", currentExcerpt)
                    codePanel("Fixed", fixedExcerpt)
                }
            currentExcerpt != null ->
                codePanel("Source", currentExcerpt)
        }
    }
}

private fun FlowContent.codePanel(
    title: String,
    excerpt: CodeExcerpt,
) {
    div("code-panel") {
        strong { +title }
        div("code-frame") {
            excerpt.lines.forEach { line ->
                codeLine(line)
            }
        }
    }
}

private fun FlowContent.codeLine(line: CodeExcerptLine) {
    div("code-line") {
        span("line-number") { +"${line.number}" }
        span {
            val start = line.highlightStartColumn
            val end = line.highlightEndColumn
            if (start == null || end == null || start >= end) {
                +line.text
            } else {
                val startIndex = (start - 1).coerceIn(0, line.text.length)
                val endIndex = (end - 1).coerceIn(startIndex, line.text.length)
                +line.text.substring(0, startIndex)
                span("marked") { +line.text.substring(startIndex, endIndex) }
                +line.text.substring(endIndex)
            }
        }
    }
}

private fun Diagnostic.codeExcerpt(): CodeExcerpt? {
    val sourceFile = file ?: return null
    val sourceRange = range ?: return null
    return sourceFile.content.toCodeExcerpt(focusRange = sourceRange, highlightRange = sourceRange)
}

private fun Diagnostic.fixedExcerpt(): CodeExcerpt? {
    val sourceFile = file ?: return null
    val fix = fixes.firstOrNull() ?: return null
    val firstEdit = fix.edits.firstOrNull() ?: return null
    val fixedContent = sourceFile.content.applyTextEdits(fix.edits) ?: return null
    val highlightRange = firstEdit.replacementRangeAfterEdit()
    return fixedContent.toCodeExcerpt(
        focusRange = firstEdit.range,
        highlightRange = highlightRange,
    )
}

private fun String.toCodeExcerpt(
    focusRange: SourceRange,
    highlightRange: SourceRange?,
): CodeExcerpt? {
    val sourceLines = removeSuffix("\n").split('\n')
    if (sourceLines.singleOrNull()?.isEmpty() == true) return null
    if (focusRange.start.line !in 1..sourceLines.size) return null

    val firstLine = (focusRange.start.line - CONTEXT_LINE_COUNT).coerceAtLeast(1)
    val lastLine = (focusRange.end.line + CONTEXT_LINE_COUNT).coerceAtMost(sourceLines.size)
    val lines =
        (firstLine..lastLine).map { lineNumber ->
            CodeExcerptLine(
                number = lineNumber,
                text = sourceLines[lineNumber - 1],
                highlightStartColumn = highlightRange?.start?.column?.takeIf { lineNumber == highlightRange.start.line },
                highlightEndColumn = highlightRange?.end?.column?.takeIf { lineNumber == highlightRange.end.line },
            )
        }
    return CodeExcerpt(lines)
}

private fun String.applyTextEdits(edits: List<TextEdit>): String? {
    val offsetEdits =
        buildList {
            edits.forEach { edit ->
                val start = offsetAt(edit.range.start) ?: return null
                val end = offsetAt(edit.range.end) ?: return null
                if (end < start) return null
                add(OffsetEdit(start = start, end = end, replacement = edit.replacement))
            }
        }.sortedBy { edit -> edit.start }
    if (offsetEdits.zipWithNext().any { (left, right) -> right.start < left.end }) return null

    val builder = StringBuilder(this)
    offsetEdits
        .asReversed()
        .forEach { edit -> builder.replace(edit.start, edit.end, edit.replacement) }
    return builder.toString()
}

private fun String.offsetAt(position: SourcePosition): Int? {
    if (position.line < 1 || position.column < 1) return null

    var line = 1
    var lineStart = 0
    while (line < position.line) {
        val newline = indexOf('\n', startIndex = lineStart)
        if (newline == -1) return null
        lineStart = newline + 1
        line++
    }

    val newline = indexOf('\n', startIndex = lineStart)
    val lineEnd = if (newline == -1) length else newline
    val offset = lineStart + position.column - 1
    if (offset > lineEnd) return null
    return offset
}

private fun TextEdit.replacementRangeAfterEdit(): SourceRange? {
    if (replacement.isEmpty() || replacement.contains('\n')) return null
    return SourceRange(
        start = range.start,
        end = SourcePosition(line = range.start.line, column = range.start.column + replacement.length),
    )
}

private data class CodeExcerpt(
    val lines: List<CodeExcerptLine>,
)

private data class CodeExcerptLine(
    val number: Int,
    val text: String,
    val highlightStartColumn: Int?,
    val highlightEndColumn: Int?,
)

private data class OffsetEdit(
    val start: Int,
    val end: Int,
    val replacement: String,
)

private const val CONTEXT_LINE_COUNT = 1

private fun Diagnostic.locationLabel(): String {
    val path = file?.path ?: "-"
    val rangeLabel = range?.toLocationLabel()
    return if (rangeLabel == null) path else "$path:$rangeLabel"
}

private fun SourceRange.toLocationLabel(): String = "${start.line}:${start.column}"
