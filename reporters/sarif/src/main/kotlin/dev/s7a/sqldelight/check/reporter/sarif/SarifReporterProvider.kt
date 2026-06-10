package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in SARIF reporter.
 */
public class SarifReporterProvider : ReporterProvider {
    override val id: String = "sarif"

    override fun create(options: Map<String, String>): Reporter = SarifReporter
}

private object SarifReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        // FIXME: Implement SARIF 2.1.0 output with rule metadata and source locations.
        output.write("""{"version":"2.1.0","runs":[]}""".toByteArray())
    }
}

