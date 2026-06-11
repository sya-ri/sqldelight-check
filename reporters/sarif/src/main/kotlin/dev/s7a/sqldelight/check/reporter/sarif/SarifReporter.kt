package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reporter that writes sqldelight-check diagnostics as SARIF.
 */
internal class SarifReporter(
    private val json: Json,
) : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(json.encodeToString(report.toSarifReport()).toByteArray())
        }
    }
}

internal const val PRETTY_PRINT_OPTION: String = "prettyPrint"
