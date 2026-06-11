# sqldelight-check hsql rules

`sqldelight-check-rules-hsql` is the HSQL-specific rule slot.

These rules are gated by `DialectCapabilities.Hsql` and cover HSQL settings and
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

- `🔘` enabled automatically
- `⚠️` diagnostics by default
- no write-task fixes

## Rule Summary

| Rule ID | Default | Fix | Purpose |
| --- | --- | --- | --- |
| `hsql:no-database-file-settings` | ⚠️ |  | Disallow database or file settings in schema and migration sources. |
| `hsql:no-system-operations` | ⚠️ |  | Disallow HSQL system operations in schema and migration sources. |
| `hsql:no-text-table-source` | ⚠️ |  | Disallow HSQL text-table sources in versioned SQLDelight files. |

## Notes

The HSQL rule set is intentionally small. It focuses on the few HSQL-specific
operations that are most likely to be accidental in SQLDelight migrations.
