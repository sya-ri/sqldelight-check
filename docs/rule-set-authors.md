# Rule Set Author Guide

sqldelight-check loads external rule sets through the `sqldelightCheckRuleSet` Gradle configuration and Java
`ServiceLoader`.

## Dependency

Custom rule set projects should compile against the rule API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-rule-api:0.2.1")
}
```

Consumers add the published rule set artifact to the checked project:

```kotlin
dependencies {
    sqldelightCheckRuleSet("com.example:my-sqldelight-rules:1.0.0")
}
```

## Provider

Implement `RuleSetProvider` and return one `RuleProvider` per rule:

```kotlin
package com.example.sqldelight.rules

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

class ExampleRuleSetProvider : RuleSetProvider {
    override val id = RuleSetId("example")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::ExampleRule),
        )
}
```

Register it in:

```text
src/main/resources/META-INF/services/dev.s7a.sqldelight.check.rule.api.RuleSetProvider
```

with the provider class name:

```text
com.example.sqldelight.rules.ExampleRuleSetProvider
```

## Rule

Rules implement `Rule` and report diagnostics through `DiagnosticReporter`.

```kotlin
package com.example.sqldelight.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

class ExampleRule : Rule {
    override val id = RuleId("example")
    override val defaultSeverity = Severity.Warning
    override val defaultEnable = true

    override fun isApplicable(context: RuleContext): Boolean =
        DialectId("example") in context.database.dialect.ids

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        // Inspect context.file.content, context.file.kind, context.facts, context.database, and context.options.
    }
}
```

`RuleContext` intentionally exposes sqldelight-check-owned models instead of SQLDelight compiler internals. This keeps
custom rules stable across supported SQLDelight `2.x` versions.

## PSI And Stable Facts

Custom rules should not depend on SQLDelight PSI or IntelliJ PSI classes. Those
types are implementation details of the SQLDelight version selected by the
checked project, so exposing them from `rule-api` would make third-party rule
sets version-sensitive.

Use `RuleContext.facts` for parser-backed structure that sqldelight-check
intends to keep stable. Use `RuleContext.file.content` and
`RuleContext.file.kind` only for conservative source-text checks such as
comments, line endings, file-kind gates, or token-level policies.

When a rule needs structure that `SqlFacts` does not expose yet, prefer adding a
stable fact type to `rule-api` over reaching into SQLDelight internals.

`rule-api` also provides small source-text helpers for rules that only need
offset-stable text checks:

- `String.rangeAtOffsets(startOffset, endOffset)` converts offsets to a `SourceRange`.
- `String.sqlTokens()` scans SQL-like identifiers outside comments and quoted text.
- `List<SqlToken>.findSourcePattern(...)` matches dialect-provided source patterns against token streams.
- `Rule.reportSqlStatementMatches(...)` tokenizes source text, splits it into statements, and reports matched ranges.
- `String.maskSqlCommentsAndQuotedText()` masks comments and quoted text while preserving offsets.
- `Map<String, String>.booleanOption(...)`, `positiveIntOption(...)`, and `commaSeparatedOption(...)` parse common rule options.

Use the token helpers for simple source-text policies before writing the same
masking, statement splitting, and range mapping by hand:

```kotlin
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.sqlite.ForeignKeysOffValue
import dev.s7a.sqldelight.check.dialects.sqlite.ForeignKeysPragmaStatementStart
import dev.s7a.sqldelight.check.dialects.sqlite.SQLiteDialectId
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

class NoForeignKeysOffRule : Rule {
    override val id: RuleId = RuleId("no-foreign-keys-off")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetDialect = SQLiteDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid disabling SQLite foreign key enforcement.",
        ) { statement ->
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                ForeignKeysPragmaStatementStart,
                ForeignKeysOffValue,
            )
        }
    }
}
```

## IDs And Configuration

Rules expose only their local ID. sqldelight-check combines the provider ID and
rule ID into the configured `rule-set:rule-name` ID:

```kotlin
sqldelightCheck {
    ruleSets {
        ruleSet("example") {
            enabled.set(Enablement.Auto)
        }
    }
    rules {
        rule("example:example") {
            severity.set(Severity.Error)
            options.put("optionName", "value")
        }
    }
}
```

Custom rules can set `defaultEnable = false` to stay disabled unless users
explicitly enable them. When `defaultEnable = true`, rules use automatic
applicability: `Rule.targetDialect` and `Rule.isApplicable(context)` decide
whether the rule runs for a database/file. Explicit `Enabled` and `Disabled`
settings remain user overrides.

## Fixes

Rules may attach fixes to diagnostics. Use safe fixes only for edits that should preserve SQL behavior, such as
whitespace, line endings, or redundant punctuation. Token rewrites should normally be unsafe so users must opt in with:

```kotlin
sqldelightCheck {
    fix {
        unsafe.set(true)
    }
}
```

## Example Project

See `examples/custom-extensions/custom-ruleset` for a runnable custom rule set loaded by
`examples/custom-extensions`.
