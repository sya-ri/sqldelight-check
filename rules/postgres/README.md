# sqldelight-check postgres rules

`sqldelight-check-rules-postgres` is the PostgreSQL-specific rule set.

These rules are gated by `DialectCapabilities.PostgreSql` and focus on schema
and migration patterns that are important for live PostgreSQL databases.

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        postgres {
            enabled.set(Enablement.Auto)
        }
    }
}
```

Rule IDs use the `postgres:<rule-name>` form.

## Configuration Model

- `🔘` enabled automatically
- `⚠️` diagnostics by default
- no write-task fixes

## Rule Summary

| Rule ID | Default | Fix | Purpose |
| --- | --- | --- | --- |
| `postgres:excessive-locks` | ⚠️ |  | Flag `CREATE INDEX` statements that commonly take strong locks. |
| `postgres:require-concurrent-index` | ⚠️ |  | Require `CREATE INDEX CONCURRENTLY` for live-table index builds. |
| `postgres:no-concurrent-index-in-transaction` | ⚠️ |  | Disallow `CREATE INDEX CONCURRENTLY` inside a transaction block. |
| `postgres:require-not-valid-constraint` | ⚠️ |  | Require `NOT VALID` when adding constraints that can validate later. |
| `postgres:no-set-not-null-on-existing-column` | ⚠️ |  | Disallow `SET NOT NULL` on existing columns without a separate validation step. |
| `postgres:no-add-column-with-volatile-default` | ⚠️ |  | Disallow `ADD COLUMN` defaults that can rewrite or evaluate many existing rows. |
| `postgres:prefer-identity-over-serial` | ⚠️ |  | Prefer `GENERATED AS IDENTITY` over PostgreSQL serial pseudo-types. |
| `postgres:no-drop-column` | ⚠️ |  | Disallow `DROP COLUMN` migrations that can break live application code. |
| `postgres:no-rename-column` | ⚠️ |  | Disallow `RENAME COLUMN` migrations that can break live application code. |
| `postgres:no-rename-table` | ⚠️ |  | Disallow `RENAME TO` migrations that can break live application code. |
| `postgres:reindex-concurrently` | ⚠️ |  | Require `REINDEX CONCURRENTLY` for live reindex operations. |
| `postgres:risky-alter-table` | ⚠️ |  | Flag `ALTER TABLE` operations that commonly take strong locks. |

## Notes

The PostgreSQL rules are migration-oriented. They intentionally avoid parsing
SQLDelight internals and instead work from SQLDelight-resolved source text.
