# Reporter Author Guide

sqldelight-check loads external reporters through the `sqldelightCheckReporter` Gradle configuration and Java
`ServiceLoader`.

## Dependency

Custom reporter projects should compile against the reporter API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-reporter-api:0.1.0")
}
```

Consumers add the published reporter artifact to the checked project:

```kotlin
dependencies {
    sqldelightCheckReporter("com.example:my-sqldelight-reporter:1.0.0")
}
```

## Provider

Implement `ReporterProvider`. Its `id` is the name users configure in `sqldelightCheck.reports`.

```kotlin
package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

class ExampleReporterProvider : ReporterProvider {
    override val id = "example"

    override fun create(options: Map<String, String>): Reporter =
        ExampleReporter(options)
}
```

Register it in:

```text
src/main/resources/META-INF/services/dev.s7a.sqldelight.check.reporter.api.ReporterProvider
```

with the provider class name:

```text
com.example.sqldelight.reporter.ExampleReporterProvider
```

## Reporter

Reporters receive a stable `Report` model and write bytes to the supplied output stream.

```kotlin
package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import java.io.OutputStream

class ExampleReporter(
    private val options: Map<String, String>,
) : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        output.writer().use { writer ->
            writer.appendLine("sqldelight-check diagnostics: ${report.summary.diagnostics}")
        }
    }
}
```

Reporter options are configured in `build.gradle.kts`:

```kotlin
sqldelightCheck {
    reports {
        report("example") {
            required.set(true)
            options.put("format", "compact")
        }
    }
}
```

Reporter output files are managed by sqldelight-check. A reporter should write one complete report to the stream and
avoid reading project files directly unless its format explicitly requires it.
