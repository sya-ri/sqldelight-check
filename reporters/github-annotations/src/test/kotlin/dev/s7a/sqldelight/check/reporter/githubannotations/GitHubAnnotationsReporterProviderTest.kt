@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.reporter.githubannotations

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

class GitHubAnnotationsReporterProviderTest {
    @Test
    fun `writes empty report`() {
        val annotations = GitHubAnnotationsReporterProvider().render(Report(diagnostics = emptyList()))

        assertEquals("", annotations)
    }

    @Test
    fun `writes escaped workflow commands`() {
        val annotations =
            GitHubAnnotationsReporterProvider().render(
                Report(
                    diagnostics =
                        listOf(
                            diagnostic(
                                severity = Severity.Warning,
                                message = "Use IS NULL, not = NULL\nSee 100% rule coverage.",
                            ),
                            diagnostic(
                                ruleId = qualifiedRuleId("standard:project"),
                                severity = Severity.Info,
                                file = null,
                                range = null,
                                message = "Project-level note",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            ::warning title=standard%3Ause-is-null,file=src/commonMain/sqldelight/Player%2CTest.sq,line=2,col=8,endLine=2,endColumn=16::Use IS NULL, not = NULL%0ASee 100%25 rule coverage.
            ::notice title=standard%3Aproject::Project-level note
            """.trimIndent() + "\n",
            annotations,
        )
    }
}

private fun GitHubAnnotationsReporterProvider.render(report: Report): String {
    val output = ByteArrayOutputStream()
    create().write(report, ByteArrayReportOutput(output))
    return output.toString()
}

private fun diagnostic(
    ruleId: QualifiedRuleId = qualifiedRuleId("standard:use-is-null"),
    severity: Severity,
    file: SourceFile? =
        SourceFile(
            path = "src/commonMain/sqldelight/Player,Test.sq",
            content = "selectByDeleted:\nSELECT * FROM player WHERE deleted_at = NULL;\n",
        ),
    range: SourceRange? =
        SourceRange(
            start = SourcePosition(line = 2, column = 8),
            end = SourcePosition(line = 2, column = 16),
        ),
    message: String,
): Diagnostic =
    Diagnostic(
        ruleId = ruleId,
        severity = severity,
        message = message,
        file = file,
        range = range,
        database = null,
        fixes = emptyList(),
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
