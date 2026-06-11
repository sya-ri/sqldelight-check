package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import kotlinx.serialization.json.Json

/**
 * Provider for the built-in JSON reporter.
 */
public class JsonReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("json")

    override fun create(options: Map<String, String>): Reporter =
        JsonReporter(
            json =
                Json {
                    prettyPrint = options[PRETTY_PRINT_OPTION]?.toBooleanStrictOrNull() ?: false
                },
        )
}
