# sqldelight-check core diagnostics

Core diagnostics are emitted by the sqldelight-check engine itself instead of a rule-set artifact.

They cover suppression hygiene that depends on the final diagnostic stream after rule execution and source-level
suppression handling.

## Rule Set ID

Core diagnostic IDs use the `core:<rule-name>` form. They can be configured with the same rule override model as
rule-set diagnostics:

```kotlin
sqldelightCheck {
    rules {
        rule("core:no-redundant-suppression") {
            enabled.set(false)
        }
    }
}
```

## Configuration Model

Every core diagnostic has:

- `🟢` in the Enable column when enabled by default.
- `❌`, `⚠️`, or `ℹ️` in the Severity column for the built-in default severity.
- `✅` or `🛠️` in the Fix column when write tasks can apply a fix.

Core diagnostics currently do not provide automatic fixes.

## Diagnostic Summary

| Rule ID | Enable | Severity | Fix | Purpose |
| --- | --- | --- | --- | --- |
| [`core:require-suppression-reason`](#corerequire-suppression-reason) | 🟢 | ⚠️ |  | Require disable directives to include a reason. |
| [`core:no-redundant-suppression`](#coreno-redundant-suppression) | 🟢 | ⚠️ |  | Report disable directives that do not suppress any diagnostics. |

## `core:require-suppression-reason`

Reports sqldelight-check disable directives without a reason.

Suppressions are part of the long-term rule configuration surface. Requiring a short reason makes exceptions easier to
audit and remove later.

Invalid:

```sql
-- sqldelight-check-disable-next-line standard:no-select-star
SELECT * FROM legacy_export;
```

Valid:

```sql
-- sqldelight-check-disable-next-line standard:no-select-star -- legacy export
SELECT * FROM legacy_export;
```

The reason must be in the same SQL line comment after a second `--`. For file or block suppressions, put the reason on
the `disable-file` or `disable` directive line:

```sql
-- sqldelight-check-disable-file -- generated fixture

-- sqldelight-check-disable standard:no-select-star -- legacy export shape
SELECT * FROM legacy_export;
-- sqldelight-check-enable standard:no-select-star
```

Fix behavior:

- No automatic fix is provided.
- Checks `disable-file`, `disable-next-line`, and `disable` directives.

## `core:no-redundant-suppression`

Reports sqldelight-check disable directives that do not suppress any diagnostics.

Unused suppressions often mean a rule violation was fixed but the suppression comment remained. Removing stale
suppressions keeps local exceptions auditable.

Invalid:

```sql
-- sqldelight-check-disable-next-line standard:no-select-star -- old export shape
SELECT id, name FROM player;
```

Valid:

```sql
-- sqldelight-check-disable-next-line standard:no-select-star -- legacy export shape
SELECT * FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Runs after normal rule diagnostics are suppressed.
- Checks `disable-file`, `disable-next-line`, and `disable` directives.
