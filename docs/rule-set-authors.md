# Rule Set Author Guide

sqldelight-check loads external rule sets through the `sqldelightCheckRuleSet` Gradle configuration and Java
`ServiceLoader`.

## Dependency

Custom rule set projects should compile against the rule API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-rule-api:0.1.0")
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
    override val id = RuleId("example:example-rule")
    override val defaultSeverity = Severity.Warning
    override val defaultEnablement = Enablement.Auto

    override fun isApplicable(context: RuleContext): Boolean =
        context.database.dialect.family == DialectFamily.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        // Inspect context.file.source, context.facts, context.database, and context.options.
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
intends to keep stable. Use `RuleContext.file.source` only for conservative
source-text checks such as comments, line endings, or token-level policies.

When a rule needs structure that `SqlFacts` does not expose yet, prefer adding a
stable fact type to `rule-api` over reaching into SQLDelight internals.

## IDs And Configuration

Use `rule-set:rule-name` IDs. The rule set part should match the provider ID:

```kotlin
sqldelightCheck {
    ruleSets {
        ruleSet("example") {
            enabled.set(Enablement.Auto)
        }
    }
    rules {
        rule("example:example-rule") {
            severity.set(Severity.Error)
            options.put("optionName", "value")
        }
    }
}
```

Custom rules can override `Rule.isApplicable(context)` to opt into dialect-, database-, or file-specific automatic
enablement. The method is consulted only when the resolved rule enablement is `Auto`; explicit `Enabled` and `Disabled`
settings remain user overrides.

## Fixes

Rules may attach fixes to diagnostics. Use safe fixes only for edits that should preserve SQL behavior, such as
whitespace, line endings, or redundant punctuation. Token rewrites should normally be unsafe so users must opt in with:

```kotlin
sqldelightCheck {
    write {
        unsafe.set(true)
    }
}
```
