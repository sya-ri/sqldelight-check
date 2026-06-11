package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

public class ExampleReporter(
    private val options: Map<String, String>,
) : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().writer().use { writer ->
            writer.appendLine(options["title"] ?: "sqldelight-check")
            writer.appendLine("diagnostics=${report.diagnostics.size}")
            report.diagnostics.forEach { diagnostic ->
                writer.appendLine("${diagnostic.ruleId?.value}: ${diagnostic.message}")
            }
        }
    }
}
