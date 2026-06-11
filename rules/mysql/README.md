# sqldelight-check mysql rules

`sqldelight-check-rules-mysql` is the MySQL-specific rule set.

These rules are gated by `DialectCapabilities.MySql` and focus on schema and
migration patterns that matter for live MySQL databases.

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        mysql {
            enabled.set(Enablement.Auto)
        }
    }
}
```

Rule IDs use the `mysql:<rule-name>` form.

## Configuration Model

- `🔘` in the Enable column when enabled automatically for MySQL dialects.
- `⚠️` in the Severity column for the built-in default severity.
- The Fix column is blank when write tasks do not attach a fix.

Built-in rules default to `Severity.Warning` in `v0.1.0`. `Severity.Error`
and `Severity.Info` are supported through user configuration.

## Rule Summary

| Rule ID | Enable | Severity | Fix | Purpose |
| --- | --- | --- | --- | --- |
| `mysql:no-utf8-charset` | 🔘 | ⚠️ |  | Prefer `utf8mb4` instead of MySQL `utf8` or `utf8mb3`. |
| `mysql:no-copy-algorithm` | 🔘 | ⚠️ |  | Disallow `ALTER TABLE ... ALGORITHM=COPY` for online migrations. |
| `mysql:no-exclusive-lock` | 🔘 | ⚠️ |  | Disallow `ALTER TABLE ... LOCK=EXCLUSIVE` for online migrations. |
| `mysql:no-replace-into` | 🔘 | ⚠️ |  | Disallow `REPLACE INTO`, which can delete and reinsert rows. |
| `mysql:no-zero-date-default` | 🔘 | ⚠️ |  | Disallow zero date defaults that fail under stricter SQL modes. |
| `mysql:no-display-width-integer` | 🔘 | ⚠️ |  | Disallow deprecated integer display widths. |
| `mysql:require-index-prefix-length` | 🔘 | ⚠️ |  | Require prefix lengths for indexes on `TEXT` and `BLOB` columns. |
| `mysql:risky-alter-table` | 🔘 | ⚠️ |  | Flag `ALTER TABLE` operations that can rebuild or strongly lock a table. |

## Notes

The MySQL rules are conservative migration checks. They target SQL patterns
that commonly surprise live deployments more than they help them.
