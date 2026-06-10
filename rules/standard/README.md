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
| `standard:no-consecutive-semicolons` | Warning / Auto | Yes | Safe | Disallow directly repeated semicolon tokens. |
| `standard:no-leading-blank-lines` | Warning / Auto | Yes | Safe | Disallow blank lines before the first content line. |
| `standard:no-space-after-opening-parenthesis` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately after `(`. |
| `standard:no-space-before-closing-parenthesis` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately before `)`. |
| `standard:no-space-before-comma` | Warning / Auto | Yes | Safe | Disallow inline whitespace before `,`. |
| `standard:no-space-before-semicolon` | Warning / Auto | Yes | Safe | Disallow inline whitespace before `;`. |
| `standard:no-tab-indentation` | Warning / Auto | Yes | Safe | Replace leading indentation tabs with spaces. |
| `standard:no-trailing-blank-lines` | Warning / Auto | Yes | Safe | Disallow blank lines after the last content line. |
| `standard:no-trailing-whitespace` | Warning / Auto | Yes | Safe | Remove spaces or tabs at line ends. |
| `standard:space-after-comma` | Warning / Auto | Yes | Safe | Require one inline space after `,` when another token follows. |
| `standard:space-around-comparison-operators` | Warning / Auto | Yes | Unsafe | Prefer one inline space around comparison operators. |

## SQLFluff References

The initial standard rules are intentionally modeled after common sqlfluff layout, capitalisation, and structure rules,
but they are not a compatibility layer for sqlfluff rule codes. sqldelight-check keeps its own IDs because the rule
engine runs in SQLDelight projects, reports SQLDelight database metadata, and needs a stable API for custom rule sets.

Useful sqlfluff concepts reflected here:

- Capitalisation checks: `standard:keyword-case`.
- Layout checks around commas, semicolons, parentheses, blank lines, and line endings: the `standard:no-*` spacing and
  blank-line rules.
- Structure checks for repeated semicolons: `standard:no-consecutive-semicolons`.

Rules that need full parse-tree knowledge, alias policy, join qualification, column ordering, indentation reflow, or
dialect-specific grammar facts should be added after the public rule model exposes SQLDelight-derived facts. Until then,
the standard rule set favors source-text checks that can skip comments and quoted text without pretending to be a SQL
parser.

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

## `standard:no-leading-blank-lines`

Reports blank lines before the first non-blank line in a file.

SQLDelight files are often navigated by declaration names and generated query names. Leading whitespace makes file
headers less predictable and adds noise to formatting diffs.

Invalid:

```sql

CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Valid:

```sql
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Fix behavior:

- Removes all blank lines before the first non-blank line.
- Does not report files that contain only blank lines.
- Safe to apply in write tasks.

## `standard:no-trailing-blank-lines`

Reports blank lines after the last non-blank line in a file.

This complements `standard:final-newline`: files should end with a single newline, not with visually empty trailing
space. The rule preserves the newline that terminates the final content line and removes the extra blank area after it.

Invalid:

```sql
SELECT 1;


```

Valid:

```sql
SELECT 1;
```

Fix behavior:

- Removes blank or whitespace-only lines after the final content line.
- Leaves the final newline itself to `standard:final-newline`.
- Safe to apply in write tasks.

## `standard:no-space-before-comma`

Reports spaces and tabs before comma tokens outside comments and quoted text.

Commas bind to the item before them in common SQL style. Keeping commas tight on the left also makes column lists,
function arguments, and `VALUES` lists easier to scan.

Invalid:

```sql
SELECT id , name
FROM player;
```

Valid:

```sql
SELECT id, name
FROM player;
```

Ignored:

```sql
-- SELECT id , name
SELECT 'id , name';
```

Fix behavior:

- Removes inline spaces and tabs immediately before `,`.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:space-after-comma`

Reports commas that are not followed by exactly one inline space when another token follows on the same line.

Invalid:

```sql
SELECT id,name
FROM player;
```

Invalid:

```sql
SELECT id,   name
FROM player;
```

Valid:

```sql
SELECT id, name
FROM player;
```

Valid:

```sql
SELECT
  id,
  name
FROM player;
```

Fix behavior:

- Inserts one space after `,` when the next token is on the same line.
- Collapses repeated spaces or tabs after `,` to one space.
- Does not require a space before `)`, `]`, `}`, `;`, or another comma.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:no-space-before-semicolon`

Reports spaces and tabs before semicolon tokens outside comments and quoted text.

SQLDelight statements commonly use semicolons as terminators. Keeping the semicolon tight to the previous token avoids
style drift between schema statements and named queries.

Invalid:

```sql
SELECT 1 ;
```

Valid:

```sql
SELECT 1;
```

Fix behavior:

- Removes inline spaces and tabs immediately before `;`.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:no-consecutive-semicolons`

Reports directly repeated semicolon tokens outside comments and quoted text.

This is a small structure rule inspired by sqlfluff's repeated-semicolon checks. In SQLDelight source, consecutive
terminators normally represent accidental empty statements rather than useful syntax.

Invalid:

```sql
SELECT 1;;
```

Valid:

```sql
SELECT 1;
```

Ignored:

```sql
-- SELECT 1;;
SELECT ';;';
```

Fix behavior:

- Removes extra adjacent semicolons and leaves the first semicolon.
- Only targets directly adjacent semicolon runs such as `;;`.
- Safe to apply in write tasks.

## `standard:no-space-after-opening-parenthesis`

Reports spaces and tabs immediately after `(` outside comments and quoted text.

The rule is intentionally inline-only. It does not object to multiline layouts where the token after `(` starts on the
next line; future formatter rules can handle indentation for those cases.

Invalid:

```sql
SELECT COUNT( id)
FROM player;
```

Valid:

```sql
SELECT COUNT(id)
FROM player;
```

Fix behavior:

- Removes inline spaces and tabs immediately after `(`.
- Does not remove newlines.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:no-space-before-closing-parenthesis`

Reports spaces and tabs immediately before `)` outside comments and quoted text.

Invalid:

```sql
SELECT COUNT(id )
FROM player;
```

Valid:

```sql
SELECT COUNT(id)
FROM player;
```

Fix behavior:

- Removes inline spaces and tabs immediately before `)`.
- Does not remove newlines.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:space-around-comparison-operators`

Reports comparison operators that do not have exactly one inline space on both sides.

Covered operators:

```text
=, !=, <>, <, <=, >, >=
```

Invalid:

```sql
SELECT *
FROM player
WHERE id=1;
```

Valid:

```sql
SELECT *
FROM player
WHERE id = 1;
```

Fix behavior:

- Replaces surrounding inline spaces or tabs with one space on each side.
- Skips operators split across lines.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- SQL dialects have operator families beyond the common comparison set.
- Some dialect-specific operators are visually similar to comparison operators.
- The current rule uses source text rather than SQLDelight PSI, so users must opt in before write tasks apply fixes.

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
