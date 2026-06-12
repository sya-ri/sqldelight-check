# Rule Set Author Guide

sqldelight-check loads external rule sets through the `sqldelightCheckRuleSet` Gradle configuration and Java
`ServiceLoader`.

## Dependency

Custom rule set projects should compile against the rule API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-rule-api:0.1.1")
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
import dev.s7a.sqldelight.check.api.DialectFamily
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
        context.database.dialect.family == DialectFamily.Named("example")

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

- `RegexRule` reports regex matches in source text after masking comments and quoted text.
- `String.rangeAtOffsets(startOffset, endOffset)` converts offsets to a `SourceRange`.
- `String.sqlTokens()` scans SQL-like identifiers outside comments and quoted text.
- `String.maskSqlCommentsAndQuotedText()` masks comments and quoted text while preserving offsets.
- `Map<String, String>.booleanOption(...)`, `positiveIntOption(...)`, and `commaSeparatedOption(...)` parse common rule options.

Use `RegexRule` for simple source-text policies before writing the same masking
and range mapping by hand:

```kotlin
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

class NoUnsafePragmaRule : RegexRule(
    ruleName = "no-unsafe-pragma",
    pattern = """\bPRAGMA\s+writable_schema\s*=\s*ON\b""",
    message = "Avoid enabling writable_schema in checked SQLDelight sources.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability("example"),
)
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
applicability: `Rule.targetCapability` and `Rule.isApplicable(context)` decide
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
