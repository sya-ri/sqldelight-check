package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import kotlinx.serialization.json.Json

/**
 * Provider for the built-in SARIF reporter.
 */
public class SarifReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("sarif")

    override fun create(options: Map<String, String>): Reporter =
        SarifReporter(
            json =
                Json {
                    prettyPrint = options[PRETTY_PRINT_OPTION]?.toBooleanStrictOrNull() ?: false
                },
        )
}
