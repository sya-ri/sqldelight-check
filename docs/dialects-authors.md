# Dialects Author Guide

sqldelight-check loads SQL dialect metadata through Java `ServiceLoader`. Built-in SQLDelight dialect metadata is
packaged as dialect modules, and third-party dialects can be added through the `sqldelightCheckDialects` Gradle
configuration.

## Dependency

Custom dialect projects should compile against the public API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-api:0.2.1")
}
```

Consumers add the published dialect artifact to the checked project:

```kotlin
dependencies {
    sqldelightCheckDialects("com.example:my-sqldelight-dialects:1.0.0")
}
```

## Provider

Implement `SqlDialectProvider` and return dialect metadata for coordinates your provider owns.

```kotlin
package com.example.sqldelight.dialects

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns

class ExampleDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != "com.example") return null
        if (coordinate.module != "example-dialect") return null

        return SqlDialect(
            ids = setOf(DialectId("example")),
            sourcePatterns =
                SqlDialectSourcePatterns(
                    patterns =
                        SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                            sourcePatterns("SAMPLE", roles = setOf(TableReferenceBoundary)) +
                            sourcePatterns(
                                "QUALIFY",
                                "MATCH RECOGNIZE",
                                roles = setOf(ClauseBoundary),
                            ),
                ),
        )
    }
}
```

Register it in:

```text
src/main/resources/META-INF/services/dev.s7a.sqldelight.check.api.SqlDialectProvider
```

with the provider class name:

```text
com.example.sqldelight.dialects.ExampleDialectProvider
```

## Resolution

sqldelight-check resolves the SQLDelight dialect artifact coordinate for each database and asks discovered dialect
providers for metadata. Providers from `sqldelightCheckDialects` are consulted before bundled providers, so a project can
override built-in SQLDelight dialect metadata when needed. Third-party providers should still return `null` for
unrelated coordinates.

When no provider resolves a coordinate, sqldelight-check falls back to `DialectId.Unknown` with
`SqlDialectSourcePatterns.SourceScannerDefault`.

## Source Scanner Patterns

`SqlDialectSourcePatterns` configures conservative source-text facts used by rules. It is not a parser grammar and does
not validate whether a statement is accepted by a specific engine version. SQLDelight's parser is responsible for
accepting or rejecting concrete SQL. Built-in presets describe broad dialect ID syntax so source-text rules do not
misread valid-looking dialect constructs. Each source pattern has one or more roles describing what the syntax means to
source-text rules. Start from `SourceScannerDefault.patterns` and add dialect-specific patterns:

```kotlin
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinModifier
import dev.s7a.sqldelight.check.api.withoutExpressions

SqlDialectSourcePatterns(
    patterns =
        SqlDialectSourcePatterns.SourceScannerDefault.patterns
            .withoutExpressions("RIGHT [OUTER] JOIN") +
            sourcePatterns("FOR", roles = setOf(TableReferenceBoundary, ClauseBoundary)) +
            sourcePatterns(
                "QUALIFY",
                "MATCH RECOGNIZE",
                "FETCH {FIRST|NEXT} [ROW|ROWS]",
                roles = setOf(ClauseBoundary),
            ) +
            sourcePatterns("NATURAL", roles = setOf(JoinModifier)),
)
```

Source patterns are shared across conservative source-text rules. For example, `TableReferenceBoundary` ends table
reference scanning, `AliasBoundary` prevents reserved constructs from being treated as implicit aliases, and
`StatementStart` marks top-level statement boundaries. Patterns support required terms, optional terms with `[TERM]`,
alternatives with `{A|B}`, and optional alternatives with `[A|B]`. Use `sourcePatterns` when adding patterns with the
same roles, and use `withoutExpressions` when a dialect removes or changes a default source pattern.

`SqlSourceStructure.parse(source, sourcePatterns)` combines these patterns with token nesting and conservative source
blocks. It returns source tokens annotated with statement index, parenthesis depth, `CASE` depth, and the dialect pattern
matches that start at each token. It also returns statement, clause, parenthesized, subquery, and `CASE` blocks with
parent-child relationships. Dialect-specific constructs such as `QUALIFY`, `FETCH {FIRST|NEXT} [ROW|ROWS]`, or custom
clause boundaries should be registered as source patterns so source scanners can use the same structure across engines.
For dialects that need different statement separators, parenthesis depth terms, paired blocks, or parenthesized block
classification, pass `blockPatterns = SqlDialectSourceBlockPatterns(...)` to `SqlDialectSourcePatterns`.
Common `SqlDialectSourcePatternRole` and `SqlSourceBlockKind` values describe rule-facing meanings. Dialects should map
their syntax to those meanings through source and block patterns. A custom dialect or custom rule can still define
additional role or block-kind implementations when it needs a meaning that the built-in rules do not share.

Built-in SQLDelight dialect metadata lives in dedicated dialect modules, with source pattern presets such as
`MySqlDialectSourcePatterns` and `PostgresDialectSourcePatterns`.
