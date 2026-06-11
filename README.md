# sqldelight-check

SQLDelight formatter and rule-based linter for `.sq` and `.sqm` files.

sqldelight-check is a Gradle plugin for projects that already use SQLDelight. It reads SQLDelight's Gradle model,
delegates parsing and validation to SQLDelight, then runs sqldelight-check rule sets over the resolved source files.

## Status

sqldelight-check is pre-release. The repository is being prepared for `v0.1.0`.

The initial release targets stable SQLDelight `2.0.x` through `2.3.x`, with opt-in verification for
`2.4.0-SNAPSHOT`, and focuses on Gradle plugin usage. There is no standalone CLI. GitHub Actions usage should run the
Gradle tasks and upload or annotate the generated reports.

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
        maybeCreate("postgres").enabled.set(Enablement.Auto)
    }

    rules {
        maybeCreate("standard:final-newline").apply {
            enabled.set(Enablement.Enabled)
            severity.set(Severity.Warning)
        }
        maybeCreate("standard:no-trailing-whitespace").severity.set(Severity.Error)
        maybeCreate("standard:max-line-length").options.put("max", "120")
        maybeCreate("standard:max-joins").options.put("max", "8")
        maybeCreate("standard:max-subquery-depth").options.put("maxDepth", "3")
        maybeCreate("standard:max-case-depth").options.put("maxDepth", "2")
    }

    databases {
        maybeCreate("Database").rules {
            maybeCreate("standard:no-trailing-whitespace").severity.set(Severity.Warning)
        }
    }

    reports {
        maybeCreate("json").apply {
            required.set(true)
            options.put("prettyPrint", "true")
        }
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

## Rule Sets

Built-in rule set artifacts:

- `sqldelight-check-rules-standard`: dialect-independent rules for `.sq` and `.sqm` files.
- `sqldelight-check-rules-postgres`: PostgreSQL-specific rules gated by `DialectCapabilities.PostgreSql`.
- `sqldelight-check-rules-mysql`: MySQL-specific rules gated by `DialectCapabilities.MySql`.
- `sqldelight-check-rules-sqlite`: SQLite-specific rules gated by `DialectCapabilities.SQLite`.
- `sqldelight-check-rules-hsql`: HSQL-specific rule-set slot gated by `DialectCapabilities.Hsql`.

Rules in dialect-specific rule sets can use SQLDelight's resolved dialect capability to stay inactive for unrelated
databases. The empty HSQL slot is published intentionally so third-party and future built-in dialect rules follow the
same shape.

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
Actions when `GITHUB_ACTIONS=true`, unless explicitly disabled. Reporter-specific options can be set with `options`;
the built-in JSON and SARIF reporters support `prettyPrint`.

Enable or disable reporters in `build.gradle.kts`:

```kotlin
sqldelightCheck {
    reports {
        maybeCreate("json").required.set(true)
        maybeCreate("sarif").required.set(true)
        maybeCreate("text").required.set(false)
        maybeCreate("html").required.set(true)
        maybeCreate("markdown").required.set(true)
        maybeCreate("github-annotations").required.set(false)

        maybeCreate("json").options.put("prettyPrint", "true")
    }
}
```

Report output examples:

```text
sqldelight-check diagnostics: 1
```

```json
{"formatVersion":"0.1.0","summary":{"diagnostics":1,"errors":1,"warnings":0,"infos":0},"diagnostics":[...]}
```

```text
::error title=standard%3Afinal-newline,file=src/main/sqldelight/com/example/Player.sq,line=3,col=3,endLine=3,endColumn=3::File should end with a newline.
```

To publish annotations in GitHub Actions, run `sqldelightCheck` and print the generated workflow command file after the
task, including on failure:

```yaml
- name: Run sqldelight-check
  id: sqldelight-check
  run: ./gradlew sqldelightCheck

- name: Publish sqldelight-check annotations
  if: always()
  run: |
    if [ -f build/reports/sqldelight-check/report.github-annotations ]; then
      cat build/reports/sqldelight-check/report.github-annotations
    fi
```

SARIF and GitHub annotation paths are written relative to `GITHUB_WORKSPACE` on GitHub Actions so uploaded reports match
the checkout root. Outside GitHub Actions, paths are relative to the Gradle root project. If the Gradle root is nested
under the repository checkout, pass an explicit report root.

When the Gradle wrapper is at the checkout root:

```shell
./gradlew -p path/to/gradle-root -PsqldelightCheck.reportRoot="$PWD" sqldelightCheck
```

When the Gradle wrapper is inside the nested Gradle root:

```shell
repo_root="$PWD"
cd path/to/gradle-root
./gradlew -PsqldelightCheck.reportRoot="$repo_root" sqldelightCheck
```

## Rule Sets

The initial standard rule set contains:

- `standard:consistent-not-equal-operator`: reports files that mix `!=` and `<>`. Unsafe fix: replace later operators
  with the first style seen in the file.
- `standard:consistent-order-by-direction`: reports `ORDER BY` clauses mixing explicit and implicit sort directions. No
  automatic fix.
- `standard:final-newline`: reports files that do not end with a newline. Safe fix: insert the final newline.
- `standard:function-name-case`: reports common SQL function names that are not uppercase. Unsafe fix: uppercase the
  function token.
- `standard:data-type-case`: reports common SQL data type names that are not uppercase. Unsafe fix: uppercase the data
  type token.
- `standard:explicit-union-operator`: reports `UNION` without explicit `ALL` or `DISTINCT`. No automatic fix.
- `standard:keyword-case`: reports common SQL keywords that are not uppercase outside comments and quoted text. Unsafe
  fix: uppercase the keyword token.
- `standard:line-ending-lf`: reports CRLF and CR line endings. Safe fix: replace them with LF.
- `standard:literal-case`: reports SQL literal tokens that are not uppercase. Unsafe fix: uppercase the literal token.
- `standard:max-blank-lines`: reports more than one consecutive blank line. Safe fix: remove extra blank lines.
- `standard:max-case-depth`: reports nested `CASE` expressions deeper than `maxDepth`. No automatic fix.
- `standard:max-joins`: reports statements with more than `max` `JOIN` clauses. No automatic fix.
- `standard:no-consecutive-semicolons`: reports directly repeated semicolon tokens. Safe fix: remove extra semicolons.
- `standard:max-subquery-depth`: reports nested `SELECT` statements deeper than `maxDepth`. No automatic fix.
- `standard:no-delete-without-where`: reports `DELETE` statements without a top-level `WHERE`. No automatic fix.
- `standard:no-leading-blank-lines`: reports blank lines before the first content line. Safe fix: remove them.
- `standard:no-leading-wildcard-like`: reports `LIKE` patterns that start with `%` or `_`. No automatic fix.
- `standard:no-space-after-dot`: reports inline whitespace after `.`. Safe fix: remove it.
- `standard:no-space-after-opening-parenthesis`: reports inline whitespace after `(`. Safe fix: remove it.
- `standard:no-space-before-closing-parenthesis`: reports inline whitespace before `)`. Safe fix: remove it.
- `standard:no-space-before-comma`: reports inline whitespace before `,`. Safe fix: remove it.
- `standard:no-space-before-dot`: reports inline whitespace before `.`. Safe fix: remove it.
- `standard:no-space-before-function-parenthesis`: reports whitespace between common SQL function names and `(`. Safe
  fix: remove it.
- `standard:no-space-before-semicolon`: reports inline whitespace before `;`. Safe fix: remove it.
- `standard:no-right-join`: reports `RIGHT JOIN` and `RIGHT OUTER JOIN`. No automatic fix.
- `standard:no-select-distinct-with-group-by`: reports `SELECT DISTINCT` statements that also contain `GROUP BY`. No
  automatic fix.
- `standard:no-select-star`: reports `SELECT *` result columns. No automatic fix.
- `standard:no-select-trailing-comma`: reports trailing commas at the end of `SELECT` clauses. Unsafe fix: remove the
  comma.
- `standard:no-tab-indentation`: reports tabs in leading indentation. Safe fix: replace indentation tabs with spaces.
- `standard:no-trailing-blank-lines`: reports blank lines after the last content line. Safe fix: remove them.
- `standard:no-trailing-whitespace`: reports spaces or tabs at the end of a line. Safe fix: remove trailing whitespace.
- `standard:no-update-without-where`: reports `UPDATE` statements without a top-level `WHERE`. No automatic fix.
- `standard:prefer-coalesce`: reports `IFNULL` and `NVL` calls. Unsafe fix: replace the function name with `COALESCE`.
- `standard:prefer-count-star`: reports `COUNT(1)` and `COUNT(0)` row counts. Unsafe fix: replace the argument with
  `*`.
- `standard:prefer-explicit-column-list-in-insert`: reports `INSERT` statements without explicit target columns. No
  automatic fix.
- `standard:require-result-column-alias`: reports computed `SELECT` result columns without aliases. No automatic fix.
- `standard:space-after-block-comment-start`: reports block comments where the opening marker is not followed by a
  space. Safe fix: insert the space.
- `standard:space-after-comma`: reports missing or repeated inline spaces after `,`. Safe fix: use one space.
- `standard:space-after-line-comment-marker`: reports line comments where `--` is not followed by a space. Safe fix:
  insert the space.
- `standard:space-around-binary-operators`: reports binary arithmetic and concatenation operators without one space on
  both sides. Unsafe fix: normalize inline spacing around the operator.
- `standard:space-around-comparison-operators`: reports comparison operators without one space on both sides. Unsafe fix:
  normalize inline spacing around the operator.
- `standard:space-before-block-comment-end`: reports block comments where the closing marker is not preceded by a space.
  Safe fix: insert the space.
- `standard:use-is-null`: reports `= NULL`, `!= NULL`, and `<> NULL` comparisons. Unsafe fix: replace the operator with
  `IS` or `IS NOT`.

Rules run after SQLDelight accepts the project input. SQLDelight parser and dialect behavior are not reimplemented by
sqldelight-check.

See [`rules/standard/README.md`](rules/standard/README.md) for detailed rule behavior and examples.

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
Custom rules can override `Rule.isApplicable(context)` to opt into dialect-, database-, or file-specific automatic
enablement. The method is consulted only when the resolved rule enablement is `Auto`; explicit `Enabled` and `Disabled`
settings remain user overrides.

Custom rule set projects should compile against:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-rule-api:0.1.0")
}
```

Custom reporter projects should compile against:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-reporter-api:0.1.0")
}
```

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

## Development

Use the Gradle wrapper:

```shell
./gradlew --no-daemon check
```

Run Qodana with the repository `qodana.yaml` configuration before release changes.
