# sqldelight-check hsql rules

`sqldelight-check-rules-hsql` is the HSQL-specific rule slot.

These rules are gated by `DialectCapability.Hsql` and cover HSQL settings and
operations that are better kept out of versioned SQLDelight schema files.

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        hsql {
            enabled.set(Enablement.Auto)
        }
    }
}
```

Rule IDs use the `hsql:<rule-name>` form.

## Configuration Model

- `🟢` in the Enable column when enabled automatically for HSQL dialects.
- `❌` or `⚠️` in the Severity column for the built-in default severity.
- The Fix column is blank when write tasks do not attach a fix.

Built-in rules use `Severity.Error` for high-risk findings and `Severity.Warning` for other visible findings.
`Severity.Info` is supported for advisory findings and user configuration.

## Rule Summary

| Rule ID | Enable | Severity | Fix | Purpose |
| --- | --- | --- | --- | --- |
| [`hsql:no-database-file-settings`](#hsqlno-database-file-settings) | 🟢 | ❌ |  | Disallow database or file settings in schema and migration sources. |
| [`hsql:no-system-operations`](#hsqlno-system-operations) | 🟢 | ❌ |  | Disallow HSQL system operations in schema and migration sources. |
| [`hsql:no-text-table-source`](#hsqlno-text-table-source) | 🟢 | ⚠️ |  | Disallow HSQL text-table sources in versioned SQLDelight files. |

## Notes

The HSQL rule set is intentionally small. It focuses on the few HSQL-specific
operations that are most likely to be accidental in SQLDelight migrations.
