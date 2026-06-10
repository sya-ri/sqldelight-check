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

        assertEquals(expectedHtml("empty-report.html"), html)
    }

    @Test
    fun `writes navigable diagnostics with source excerpts`() {
        val html =
            HtmlReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                ruleId = "standard:no-select-trailing-comma",
                                severity = Severity.Error,
                                message = "Remove the trailing comma from the SELECT list.",
                                path = "src/commonMain/sqldelight/Team.sq",
                                content =
                                    """
                                    selectTeams:
                                    SELECT id,
                                           name,
                                    FROM team;
                                    """.trimIndent() + "\n",
                                range = SourceRange(
                                    start = SourcePosition(line = 3, column = 12),
                                    end = SourcePosition(line = 3, column = 13),
                                ),
                                fixTitle = "Remove trailing comma",
                                replacement = "",
                            ),
                            diagnostic(
                                ruleId = "standard:use-is-null",
                                severity = Severity.Warning,
                                message = "Use `IS NULL` instead of = NULL & keep <safe>.",
                                replacement = "IS NULL",
                            ),
                            diagnostic(
                                ruleId = "standard:keyword-case",
                                severity = Severity.Info,
                                message = "Use uppercase SQL keywords.",
                                path = "src/commonMain/sqldelight/Search.sq",
                                content =
                                    """
                                    searchPlayers:
                                    select * from player;
                                    """.trimIndent() + "\n",
                                range = SourceRange(
                                    start = SourcePosition(line = 2, column = 1),
                                    end = SourcePosition(line = 2, column = 7),
                                ),
                                fixTitle = "Uppercase keyword",
                                replacement = "SELECT",
                            ),
                        ),
                ),
            )

        assertEquals(expectedHtml("diagnostics-report.html"), html)
    }
}

private fun HtmlReporterProvider.render(report: Report): String {
    val output = ByteArrayOutputStream()
    create().write(report, output)
    return output.toString()
}

private fun expectedHtml(name: String): String =
    requireNotNull(HtmlReporterProviderTest::class.java.getResource(name)) {
        "Missing HTML reporter test resource: $name"
    }.readText()

private fun diagnostic(
    ruleId: String = "standard:use-is-null",
    severity: Severity = Severity.Warning,
    message: String,
    path: String = "src/commonMain/sqldelight/Player.sq",
    content: String = "selectByDeleted:\nSELECT * FROM player WHERE deleted_at = NULL;\n",
    range: SourceRange = SourceRange(
        start = SourcePosition(line = 2, column = 39),
        end = SourcePosition(line = 2, column = 45),
    ),
    fixTitle: String = "Use IS NULL",
    replacement: String = "IS NULL",
): Diagnostic =
    Diagnostic(
        ruleId = RuleId(ruleId),
        severity = severity,
        message = message,
        file = SourceFile(
            path = path,
            content = content,
        ),
        range = range,
        database = null,
        fixes =
            listOf(
                Fix(
                    title = fixTitle,
                    safety = FixSafety.Safe,
                    edits =
                        listOf(
                            TextEdit(
                                range = range,
                                replacement = replacement,
                            ),
                        ),
                ),
            ),
    )
