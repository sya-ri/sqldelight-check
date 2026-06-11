# sqldelight-check sqlite rules

`sqldelight-check-rules-sqlite` is the SQLite-specific rule set.

These rules are gated by `DialectCapabilities.SQLite` and focus on schema and
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

- `🔘` enabled automatically
- `⚠️` diagnostics by default
- no write-task fixes

## Rule Summary

| Rule ID | Default | Fix | Purpose |
| --- | --- | --- | --- |
| `sqlite:consistent-conflict-resolution` | ⚠️ |  | Require conflict resolution syntax to stay consistent within a file. |
| `sqlite:foreign-keys-restored` | ⚠️ |  | Require foreign key enforcement to be restored after temporary disablement. |
| `sqlite:prefer-integer-primary-key` | ⚠️ |  | Prefer exact `INTEGER PRIMARY KEY` for rowid alias behavior. |
| `sqlite:no-autoincrement-without-need` | ⚠️ |  | Disallow `AUTOINCREMENT` when the stricter rowid behavior is not needed. |
| `sqlite:no-alter-table-complex-change` | ⚠️ |  | Disallow complex `ALTER TABLE` changes that SQLite cannot apply in place. |
| `sqlite:prefer-without-rowid-for-composite-pk` | ⚠️ |  | Prefer `WITHOUT ROWID` for tables with composite primary keys. |

## Notes

SQLite rules stay close to migration and schema safety. They are intentionally
small and specific rather than trying to model SQLite as a full formatter.
