# sqldelight-check standard rules

`sqldelight-check-rules-standard` is the built-in dialect-independent rule set.

The rule set is intentionally conservative. SQLDelight still owns SQL parsing, dialect validation, type resolution, and
compiler diagnostics. Standard rules only operate on the stable source text model exposed by `rule-api`, so early rules
focus on whitespace, line endings, and token-level checks that can be implemented without depending on SQLDelight
compiler internals.

## Rule Set ID

```kotlin
sqldelightCheck {
    ruleSets {
        maybeCreate("standard")
    }
}
```

Rule IDs use the `standard:<rule-name>` form.

## Configuration Model

Every rule has:

- default enablement: `Auto`
- default severity: `Warning`
- optional fixes attached to diagnostics

Users can override enablement and severity in `build.gradle.kts`:

```kotlin
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Severity

sqldelightCheck {
    rules {
        maybeCreate("standard:keyword-case").apply {
            enabled.set(Enablement.Enabled)
            severity.set(Severity.Info)
        }
    }
}
```

Database-specific overrides use the SQLDelight database name:

```kotlin
sqldelightCheck {
    databases {
        maybeCreate("MainDatabase").rules {
            maybeCreate("standard:no-trailing-whitespace").severity.set(Severity.Error)
        }
    }
}
```

## Fix Safety

Write tasks apply safe fixes by default:

```shell
./gradlew sqldelightCheckWrite
```

Unsafe fixes require explicit opt-in:

```kotlin
sqldelightCheck {
    write {
        unsafe.set(true)
    }
}
```

The standard rule set uses two safety levels:

- `Safe`: whitespace or line-ending edits that are intended to preserve SQL behavior.
- `Unsafe`: token edits that are usually style-only but may affect projects that intentionally use keyword-like
  identifiers or dialect-specific edge cases.

## Rule Summary

| Rule ID | Default | Fix | Safety | Purpose |
| --- | --- | --- | --- | --- |
| `standard:final-newline` | Warning / Auto | Yes | Safe | Require files to end with one LF newline. |
| `standard:keyword-case` | Warning / Auto | Yes | Unsafe | Prefer uppercase common SQL keywords outside comments and quoted text. |
| `standard:line-ending-lf` | Warning / Auto | Yes | Safe | Replace CRLF or CR line endings with LF. |
| `standard:max-blank-lines` | Warning / Auto | Yes | Safe | Disallow more than one consecutive blank line. |
| `standard:no-tab-indentation` | Warning / Auto | Yes | Safe | Replace leading indentation tabs with spaces. |
| `standard:no-trailing-whitespace` | Warning / Auto | Yes | Safe | Remove spaces or tabs at line ends. |

## `standard:final-newline`

Reports a non-empty `.sq` or `.sqm` file when it does not end with `\n`.

This rule keeps diffs stable and avoids tool-specific behavior around the last line of a file. It mirrors the common
text-file convention used by formatters and linters.

Invalid:

```sql
SELECT 1;
```

Valid:

```sql
SELECT 1;

```

The valid example has an LF after the semicolon.

Fix behavior:

- Inserts `\n` at end of file.
- Does not report empty files.
- Safe to apply in write tasks.

## `standard:line-ending-lf`

Reports CRLF (`\r\n`) and lone CR (`\r`) line endings.

SQLDelight projects commonly run across macOS, Linux, and CI environments. Normalizing to LF keeps generated reports,
line/column locations, and review diffs predictable.

Invalid:

```text
CREATE TABLE player (\r\n
  id INTEGER NOT NULL PRIMARY KEY\r\n
);\r\n
```

Valid:

```sql
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Fix behavior:

- Replaces CRLF and CR with LF.
- Reports each non-LF line ending.
- Safe to apply in write tasks.

## `standard:no-trailing-whitespace`

Reports spaces and tabs at the end of a line.

This is intentionally limited to spaces and tabs. It does not try to classify every Unicode whitespace character,
because SQL dialects and editors can disagree on how those characters should be displayed or preserved.

Invalid:

```sql
SELECT 1;··
```

Valid:

```sql
SELECT 1;
```

In the invalid example, `··` represents two trailing spaces.

Fix behavior:

- Removes trailing spaces and tabs from each affected line.
- Keeps the line ending unchanged.
- Safe to apply in write tasks.

## `standard:no-tab-indentation`

Reports tab characters in leading indentation.

The rule only checks leading whitespace. Tabs inside SQL string literals, comments, quoted identifiers, or expression
text are not targeted by this rule unless they are part of indentation at the beginning of a line.

Invalid:

```sql
→SELECT *
→FROM player;
```

Valid:

```sql
    SELECT *
    FROM player;
```

In the invalid example, `→` represents a tab.

Fix behavior:

- Replaces each indentation tab with four spaces.
- Leaves non-leading tab characters alone.
- Safe to apply in write tasks.

Rationale:

- This is a formatting baseline, not a full indentation engine.
- A future formatter can replace this with dialect-aware indentation rules while keeping this rule as a simple guard.

## `standard:max-blank-lines`

Reports runs with more than one consecutive blank line.

The rule allows one blank line because SQLDelight files often group schema declarations, queries, and migrations into
small sections. Larger gaps usually come from editing churn and make files harder to scan.

Invalid:

```sql
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);


selectAll:
SELECT * FROM player;
```

Valid:

```sql
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);

selectAll:
SELECT * FROM player;
```

Fix behavior:

- Removes extra blank lines after the first blank line in a run.
- Treats whitespace-only lines as blank.
- Safe to apply in write tasks.

## `standard:keyword-case`

Reports common SQL keywords that are not uppercase.

This rule is inspired by sqlfluff's keyword capitalisation checks, but sqldelight-check keeps the first version simpler:
it uses a conservative token scanner over SQLDelight source text and intentionally skips comments, string literals,
double-quoted identifiers, backtick identifiers, and bracket identifiers.

Invalid:

```sql
select id, name
from player
where id is not null;
```

Valid:

```sql
SELECT id, name
FROM player
WHERE id IS NOT NULL;
```

Ignored:

```sql
-- select from where
SELECT 'select from where';
SELECT "from" FROM player;
```

Fix behavior:

- Replaces recognized keyword tokens with uppercase text.
- Marks fixes as `Unsafe`.
- Does not apply during normal write tasks unless `write.unsafe.set(true)` is configured.

Why unsafe:

- SQL keywords are generally case-insensitive, but some dialects allow keyword-like identifiers in contexts that are
  difficult to classify without exposing SQLDelight PSI.
- The first implementation avoids comments and quoted text, but it does not yet use SQLDelight's AST to prove that a
  token is syntactically a keyword.

Current keyword coverage:

```text
ADD, ALTER, AND, AS, ASC, BETWEEN, BY, CASE, CHECK, COLUMN, CONSTRAINT, CREATE,
DEFAULT, DELETE, DESC, DISTINCT, DROP, ELSE, END, EXISTS, FOREIGN, FROM, GROUP,
HAVING, IN, INDEX, INNER, INSERT, INTO, IS, JOIN, KEY, LEFT, LIKE, LIMIT, NOT,
NULL, ON, OR, ORDER, OUTER, PRIMARY, REFERENCES, RIGHT, SELECT, SET, TABLE,
THEN, UNION, UNIQUE, UPDATE, VALUES, WHEN, WHERE
```

## Current Boundaries

The standard rule set does not yet include:

- dialect-specific keyword lists
- SQLDelight PSI-backed AST rules
- indentation reflow
- column layout alignment
- aliasing rules
- query naming rules
- migration-number rules

Those rules should be added when the public rule model can expose enough SQLDelight-derived facts without coupling custom
rules to SQLDelight internals.
