# sqldelight-check

SQLDelight formatter and rule-based linter for `.sq` and `.sqm` files.

sqldelight-check is a Gradle plugin for projects that already use SQLDelight. It reads SQLDelight's Gradle model,
delegates parsing and validation to SQLDelight, then runs sqldelight-check rule sets over the resolved source files.

## Status

sqldelight-check is pre-release. The repository is being prepared for `v0.1.0`.

The initial release targets stable SQLDelight `2.0.x` through `2.3.x`, with opt-in verification for
`2.4.0-SNAPSHOT`, and focuses on Gradle plugin usage. There is no standalone CLI.

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

The plugin installs the standard rule set and standard reporters by default. Dialect-specific rule sets are published
separately so projects can opt into rules that only make sense for a specific SQLDelight dialect.

## Run

Use the Gradle tasks installed by the plugin:

```shell
./gradlew sqldelightCheck
./gradlew sqldelightCheckWrite
```

- `sqldelightCheck`: run SQLDelight analysis, rules, and reports without modifying files.
- `sqldelightCheckWrite`: apply allowed fixes, re-run analysis, then write reports.
- `sqldelightLint` and `sqldelightFormat`: stable task aliases for the command model.
- `sqldelightLintWrite` and `sqldelightFormatWrite`: write-task aliases.

The first rules are lint-style rules with safe fixes; SQL formatting rules will be added behind the same task model.

## Configure

Configure rule sets, rules, reports, write safety, and database-specific overrides in `build.gradle.kts`:

```kotlin
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.Severity

sqldelightCheck {
    ruleSets {
        standard {
            enabled.set(Enablement.Auto)
        }
        postgres {
            enabled.set(Enablement.Auto)
        }
    }

    rules {
        rule("standard:final-newline") {
            enabled.set(Enablement.Enabled)
            severity.set(Severity.Warning)
        }
        rule("standard:no-trailing-whitespace") {
            severity.set(Severity.Error)
        }
        rule("standard:max-line-length") {
            options.put("max", "120")
        }
        rule("standard:max-joins") {
            options.put("max", "8")
        }
        rule("standard:max-subquery-depth") {
            options.put("maxDepth", "3")
        }
        rule("standard:max-case-depth") {
            options.put("maxDepth", "2")
        }
    }

    databases {
        database("Database") {
            rules {
                rule("standard:no-trailing-whitespace") {
                    severity.set(Severity.Warning)
                }
            }
        }
    }

    reports {
        json {
            required.set(true)
            options.put("prettyPrint", "true")
        }
        sarif {
            required.set(true)
        }
        text {
            required.set(true)
        }
        html {
            required.set(false)
        }
        markdown {
            required.set(false)
        }
    }

    write {
        unsafe.set(false)
    }

    logLevel.set(LogLevel.Verbose)
}
```

Enablement values:

- `Auto`: let sqldelight-check decide from the rule default and applicability.
- `Enabled`: run the rule or rule set.
- `Disabled`: do not run the rule or rule set.

Rule-level explicit enablement overrides a rule set default. Severity values are `Info`, `Warning`, and `Error`; error
diagnostics fail check tasks after reports are written.

Log levels control task output detail:

- `Info`: task summary only.
- `Verbose`: summary plus resolved file inventory.
- `Debug`: summary, resolved file inventory, and per-file rule traces.

## Disable Diagnostics

Use SQL comments when a source file needs a local rule suppression:

```sql
-- sqldelight-check-disable-next-line standard:use-is-null
selectById:
SELECT *
FROM player
WHERE deleted_at = NULL;

-- sqldelight-check-disable standard:no-select-star
selectEverything:
SELECT *
FROM player;
-- sqldelight-check-enable standard:no-select-star
```

Supported directives:

- `-- sqldelight-check-disable-next-line [rule-id,...]`: suppress matching rule diagnostics on the next line.
- `-- sqldelight-check-disable-file [rule-id,...]`: suppress matching rule diagnostics for the whole file.
- `-- sqldelight-check-disable [rule-id,...]`: start suppressing matching rule diagnostics.
- `-- sqldelight-check-enable [rule-id,...]`: stop the matching `disable` block. Without rule IDs, all active disables stop.

Omitting rule IDs suppresses all rule diagnostics covered by that directive. Directives do not suppress SQLDelight
parser or compiler diagnostics.

## Rule Sets

Built-in rule set artifacts:

- `sqldelight-check-rules-standard`: dialect-independent rules for `.sq` and `.sqm` files.
- `sqldelight-check-rules-postgres`: PostgreSQL-specific rules gated by `DialectCapabilities.PostgreSql`.
- `sqldelight-check-rules-mysql`: MySQL-specific rules gated by `DialectCapabilities.MySql`.
- `sqldelight-check-rules-sqlite`: SQLite-specific rules gated by `DialectCapabilities.SQLite`.
- `sqldelight-check-rules-hsql`: HSQL-specific rule-set slot gated by `DialectCapabilities.Hsql`.

See the rule-set README files for rule behavior, options, and examples:

- [Standard rules](rules/standard/README.md)
- [PostgreSQL rules](rules/postgres/README.md)
- [MySQL rules](rules/mysql/README.md)
- [SQLite rules](rules/sqlite/README.md)
- [HSQL rules](rules/hsql/README.md)

Rules run after SQLDelight accepts the project input. SQLDelight parser and dialect behavior are not reimplemented by
sqldelight-check.

sqldelight-check's standard rules are inspired by the practical SQL linting vocabulary established by
[SQLFluff](https://sqlfluff.com/). Thanks to the SQLFluff project for making those rule categories familiar and useful
to SQL users. sqldelight-check keeps separate rule IDs and behavior because it runs inside SQLDelight projects and uses
SQLDelight database metadata.

## Reports

Built-in reporters are installed with the Gradle plugin:

- `json`: machine-readable summary and diagnostics.
- `sarif`: SARIF 2.1.0 results for code scanning and artifact upload.
- `text`: compact human-readable diagnostic count.
- `html`: navigable diagnostics table for uploaded CI artifacts.
- `markdown`: summary and diagnostics table suitable for GitHub Actions job summaries.
- `github-annotations`: GitHub Actions workflow command annotations for changed files and check logs.

Default report outputs are written under `build/reports/sqldelight-check/`. JSON, SARIF, and text are enabled by
default. HTML and Markdown are available but disabled by default. GitHub annotations are enabled automatically on GitHub
Actions when `GITHUB_ACTIONS=true`, unless explicitly disabled.

See [Report Outputs](docs/reports.md) for GitHub Actions snippets and examples. The HTML report is intended to be the
primary visual artifact for CI uploads.

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

Authoring guides:

- [Rule Set Author Guide](docs/rule-set-authors.md)
- [Reporter Author Guide](docs/reporter-authors.md)

## SQLDelight And Dialects

sqldelight-check reads SQLDelight database tasks from Gradle and supports multiple SQLDelight databases in one project.
Dialect metadata is inferred from SQLDelight's dialect configuration and passed to rules as stable sqldelight-check API
data.

The `v0.1.0` core analyzer targets stable SQLDelight `2.0.x`, `2.1.x`, `2.2.x`, and `2.3.x`. `2.4.0-SNAPSHOT` is
covered by an opt-in compatibility test because the final SQLDelight 2.4.0 Gradle plugin marker is not available yet.

## Limitations

- There is no standalone CLI.
- SQLDelight parser/compiler diagnostics use best-effort source ranges derived from SQLDelight error output.
- `sqldelightLint` and `sqldelightFormat` currently share the same rule execution path.
