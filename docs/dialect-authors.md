# Dialect Author Guide

sqldelight-check loads SQL dialect metadata through Java `ServiceLoader`. Built-in SQLDelight dialect metadata is
packaged as a dialect module, and a third-party dialect can be added through the `sqldelightCheckDialect` Gradle
configuration.

## Dependency

Custom dialect projects should compile against the public API:

```kotlin
dependencies {
    compileOnly("dev.s7a:sqldelight-check-api:0.1.0")
}
```

Consumers add the published dialect artifact to the checked project:

```kotlin
dependencies {
    sqldelightCheckDialect("com.example:my-sqldelight-dialect:1.0.0")
}
```

## Provider

Implement `SqlDialectProvider` and return dialect metadata for coordinates your provider owns.

```kotlin
package com.example.sqldelight.dialect

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourceKeywords

class ExampleDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != "com.example") return null
        if (coordinate.module != "example-dialect") return null

        return SqlDialect(
            family = DialectFamily.Custom,
            capabilities = setOf(DialectCapability("example")),
            sourceKeywords =
                SqlDialectSourceKeywords.SourceScannerDefault.extend(
                    addTableReferenceBoundaryKeywords = setOf("sample"),
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
com.example.sqldelight.dialect.ExampleDialectProvider
```

## Resolution

sqldelight-check resolves the SQLDelight dialect artifact coordinate for each database and asks discovered dialect
providers for metadata. Providers from `sqldelightCheckDialect` are consulted before bundled providers, so a project can
override built-in SQLDelight dialect metadata when needed. Third-party providers should still return `null` for
unrelated coordinates.

When no provider resolves a coordinate, sqldelight-check falls back to `DialectFamily.Custom` with
`SqlDialectSourceKeywords.SourceScannerDefault`.

## Source Scanner Keywords

`SqlDialectSourceKeywords` configures conservative source-text facts used by rules. It is not a parser grammar. Start
from `SourceScannerDefault` and use `extend(...)` for dialect-specific additions or removals:

```kotlin
SqlDialectSourceKeywords.SourceScannerDefault.extend(
    addTableReferenceBoundaryKeywords = setOf("for"),
    removeJoinModifierKeywords = setOf("right"),
)
```

Built-in SQLDelight dialect metadata uses dedicated source keyword presets such as `SqlDialectSourceKeywords.MySql` and
`SqlDialectSourceKeywords.PostgreSql`.
