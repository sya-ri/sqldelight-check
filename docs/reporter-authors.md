# Reporter Author Guide

sqldelight-check loads external reporters through the `sqldelightCheckReporter` Gradle configuration and Java
`ServiceLoader`.

## Dependency

Custom reporter projects should compile against the reporter API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-reporter-api:0.2.1")
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
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

class ExampleReporterProvider : ReporterProvider {
    override val id = ReporterId("example")

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

Reporters receive a stable `Report` model and write files through `ReportOutput`.

```kotlin
package com.example.sqldelight.reporter

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

class ExampleReporter(
    private val options: Map<String, String>,
) : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().writer().use { writer ->
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
            outputFile.set(layout.buildDirectory.file("reports/sqldelight-check/example.txt"))
            outputDirectory.set(layout.buildDirectory.dir("reports/sqldelight-check/example"))
        }
    }
}
```

## Typed Gradle DSL

Reporter providers receive options as `Map<String, String>`, so every reporter can be configured with `options`.
If a reporter is bundled with a Gradle plugin, prefer exposing common options through a typed extension that extends
`ReporterExtension`, while still mapping those values to the reporter's string options before creating the reporter.
Override `resolvedOptions()` to make that mapping reusable by the Gradle task.

Built-in reporters follow this pattern: `JsonReporterExtension` and `SarifReporterExtension` expose
`prettyPrint: Property<Boolean>`, while `TextReporterExtension`, `HtmlReporterExtension`,
`MarkdownReporterExtension`, and `GitHubAnnotationsReporterExtension` exist as reporter-specific extension types even
when they currently only inherit the shared reporter settings.

```kotlin
public open class ExampleReporterExtension
    @Inject
    constructor(
        name: String,
        objects: ObjectFactory,
    ) : ReporterExtension(name, objects) {
        public val compact: Property<Boolean> =
            objects.property(Boolean::class.java)

        override fun resolvedOptions(): Map<String, String> =
            buildMap {
                putAll(super.resolvedOptions())
                if (compact.isPresent) {
                    put("compact", compact.get().toString())
                }
            }
    }
```

Users can then configure the typed option without stringly-typed Gradle code:

```kotlin
sqldelightCheck {
    reports {
        json {
            prettyPrint.set(true)
        }
    }
}
```

Reporter output files are managed by sqldelight-check. Single-file reporters should write to `output.file()`.
Reporters that need assets or shards can write relative paths with `output.file("assets/report.css")`. A reporter
should avoid reading project files directly unless its format explicitly requires it.

## Example Project

See `examples/custom-extensions/custom-reporter` for a runnable custom reporter loaded by
`examples/custom-extensions`.
