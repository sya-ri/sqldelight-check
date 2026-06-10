# sqldelight-check

SQLDelight formatter and rule-based linter for `.sq` and `.sqm` files.

sqldelight-check is a Gradle plugin for projects that already use SQLDelight. It reads SQLDelight's Gradle model,
delegates parsing and validation to SQLDelight, then runs sqldelight-check rule sets over the resolved source files.

## Status

sqldelight-check is pre-release. The repository is being prepared for `v0.1.0`.

The initial release targets SQLDelight `2.3.2` and focuses on Gradle plugin usage. There is no standalone CLI. GitHub
Actions usage should run the Gradle tasks and upload or annotate the generated reports.

## Install

Apply SQLDelight and sqldelight-check to the same Gradle project:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("app.cash.sqldelight") version "2.3.2"
    id("dev.s7a.sqldelight.check") version "0.1.0"
}

repositories {
    mavenCentral()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
            srcDirs("src/main/sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
        }
    }
}
```

The plugin installs the standard rule set and standard reporters by default.

## Tasks

- `sqldelightCheck`: run SQLDelight analysis, rules, and reports without modifying files.
- `sqldelightCheckWrite`: apply allowed fixes, re-run analysis, then write reports.
- `sqldelightLint`: currently equivalent to `sqldelightCheck`.
- `sqldelightLintWrite`: currently equivalent to `sqldelightCheckWrite`.
- `sqldelightFormat`: currently equivalent to `sqldelightCheck`.
- `sqldelightFormatWrite`: currently equivalent to `sqldelightCheckWrite`.

The separate lint and format task names are stable entry points for the command model. The first rules are lint-style
rules with safe fixes; SQL formatting rules will be added behind the same task model.

## Configuration

Configure rule sets, rules, reports, write safety, and database-specific overrides in `build.gradle.kts`:

```kotlin
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Severity

sqldelightCheck {
    ruleSets {
        maybeCreate("standard").enabled.set(Enablement.Auto)
    }

    rules {
        maybeCreate("standard:final-newline").apply {
            enabled.set(Enablement.Enabled)
            severity.set(Severity.Warning)
        }
        maybeCreate("standard:no-trailing-whitespace").severity.set(Severity.Error)
    }

    databases {
        maybeCreate("Database").rules {
            maybeCreate("standard:no-trailing-whitespace").severity.set(Severity.Warning)
        }
    }

    reports {
        maybeCreate("json").required.set(true)
        maybeCreate("sarif").required.set(true)
        maybeCreate("text").required.set(true)
        maybeCreate("html").required.set(false)
        maybeCreate("markdown").required.set(false)
    }

    write {
        unsafe.set(false)
    }
}
```

Enablement values:

- `Auto`: let sqldelight-check decide from the rule default and applicability.
- `Enabled`: run the rule or rule set.
- `Disabled`: do not run the rule or rule set.

Rule-level explicit enablement overrides a rule set default. Severity values are `Info`, `Warning`, and `Error`; error
diagnostics fail check tasks after reports are written.

## Reports

Built-in reporters are installed with the Gradle plugin:

- `json`
- `sarif`
- `text`
- `html`
- `markdown`

Default report outputs are written under `build/reports/sqldelight-check/`. JSON, SARIF, and text are enabled by
default. HTML and Markdown are available but disabled by default.

## Rule Sets

The initial standard rule set contains:

- `standard:final-newline`: reports files that do not end with a newline. Safe fix: insert the final newline.
- `standard:no-trailing-whitespace`: reports spaces or tabs at the end of a line. Safe fix: remove trailing whitespace.

Rules run after SQLDelight accepts the project input. SQLDelight parser and dialect behavior are not reimplemented by
sqldelight-check.

## Write Safety

Write tasks apply the first allowed fix from each diagnostic. Safe fixes are enabled by default. Unsafe fixes require:

```kotlin
sqldelightCheck {
    write {
        unsafe.set(true)
    }
}
```

Invalid edits and overlapping edits are skipped. When a write task changes files, sqldelight-check runs analysis again
and writes reports for the remaining diagnostics.

## Custom Providers

External rule sets and reporters are loaded through Gradle configurations:

```kotlin
dependencies {
    sqldelightCheckRuleSet("com.example:my-sqldelight-rules:1.0.0")
    sqldelightCheckReporter("com.example:my-sqldelight-reporter:1.0.0")
}
```

Rule set artifacts provide `dev.s7a.sqldelight.check.rule.api.RuleSetProvider` via Java `ServiceLoader`.
Reporter artifacts provide `dev.s7a.sqldelight.check.reporter.api.ReporterProvider` via Java `ServiceLoader`.

## SQLDelight And Dialects

sqldelight-check reads SQLDelight database tasks from Gradle and supports multiple SQLDelight databases in one project.
Dialect metadata is inferred from SQLDelight's dialect configuration and passed to rules as stable sqldelight-check API
data.

The `v0.1.0` adapter targets SQLDelight `2.3.2`.

## Limitations

- There is no standalone CLI.
- SQLDelight diagnostic source ranges are still coarse when SQLDelight reports parser/compiler failures.
- `sqldelightLint` and `sqldelightFormat` currently share the same rule execution path.
- HTML and Markdown reporters are intentionally simple in the initial release.

## Development

Use the Gradle wrapper:

```shell
./gradlew --no-daemon check
```

Run Qodana with the repository `qodana.yaml` configuration before release changes.
