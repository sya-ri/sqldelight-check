package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

public class ExampleReporterProvider : ReporterProvider {
    override val id: String = "example"

    override fun create(options: Map<String, String>): Reporter =
        ExampleReporter(options)
}
