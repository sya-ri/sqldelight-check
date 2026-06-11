# sqldelight-check sqlite rules

`sqldelight-check-rules-sqlite` is the SQLite-specific rule set.

These rules are gated by `DialectCapability.SQLite` and focus on schema and
migration patterns that matter for SQLite rowid behavior and table rebuilds.

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        sqlite {
            enabled.set(Enablement.Auto)
        }
    }
}
```

Rule IDs use the `sqlite:<rule-name>` form.

## Configuration Model

- `🔘` in the Enable column when enabled automatically for SQLite dialects.
- `⚠️` in the Severity column for the built-in default severity.
- The Fix column is blank when write tasks do not attach a fix.

Built-in rules default to `Severity.Warning` in `v0.1.0`. `Severity.Error`
and `Severity.Info` are supported through user configuration.

## Rule Summary

| Rule ID | Enable | Severity | Fix | Purpose |
| --- | --- | --- | --- | --- |
| [`sqlite:consistent-conflict-resolution`](#sqliteconsistent-conflict-resolution) | 🔘 | ⚠️ |  | Require conflict resolution syntax to stay consistent within a file. |
| [`sqlite:foreign-keys-restored`](#sqliteforeign-keys-restored) | 🔘 | ⚠️ |  | Require foreign key enforcement to be restored after temporary disablement. |
| [`sqlite:prefer-integer-primary-key`](#sqliteprefer-integer-primary-key) | 🔘 | ⚠️ |  | Prefer exact `INTEGER PRIMARY KEY` for rowid alias behavior. |
| [`sqlite:no-autoincrement-without-need`](#sqliteno-autoincrement-without-need) | 🔘 | ⚠️ |  | Disallow `AUTOINCREMENT` when the stricter rowid behavior is not needed. |
| [`sqlite:no-alter-table-complex-change`](#sqliteno-alter-table-complex-change) | 🔘 | ⚠️ |  | Disallow complex `ALTER TABLE` changes that SQLite cannot apply in place. |
| [`sqlite:prefer-without-rowid-for-composite-pk`](#sqliteprefer-without-rowid-for-composite-pk) | 🔘 | ⚠️ |  | Prefer `WITHOUT ROWID` for tables with composite primary keys. |

## Notes

SQLite rules stay close to migration and schema safety. They are intentionally
small and specific rather than trying to model SQLite as a full formatter.
