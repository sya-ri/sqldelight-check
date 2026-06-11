package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId

public class ExampleReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("example")

    override fun create(options: Map<String, String>): Reporter =
        ExampleReporter(options)
}
