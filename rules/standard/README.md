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
| `standard:blocked-words` | Warning / Auto | No | None | Report configured blocked words outside comments and quoted text by default. |
| `standard:clause-keyword-newline` | Warning / Auto | No | None | Require major top-level `SELECT` clause keywords to start their own line in multiline statements. |
| `standard:consistent-column-references` | Warning / Auto | No | None | Disallow mixing ordinal and named references in `GROUP BY` and `ORDER BY`. |
| `standard:consistent-not-equal-operator` | Warning / Auto | Yes | Unsafe | Keep `!=` and `<>` not-equal operators consistent within a file. |
| `standard:consistent-order-by-direction` | Warning / Auto | No | None | Require all or none of the `ORDER BY` items to specify `ASC` or `DESC`. |
| `standard:consistent-set-operation-column-count` | Warning / Auto | No | None | Require adjacent set-operation SELECT lists to return the same number of columns. |
| `standard:data-type-case` | Warning / Auto | Yes | Unsafe | Prefer uppercase common SQL data type names outside comments and quoted text. |
| `standard:explicit-cross-join` | Warning / Auto | No | None | Require `CROSS JOIN` when a join has no `ON` or `USING` condition. |
| `standard:explicit-inner-join` | Warning / Auto | No | None | Require `INNER JOIN` instead of bare `JOIN` when `ON` or `USING` is present. |
| `standard:explicit-union-operator` | Warning / Auto | No | None | Require `UNION ALL` or `UNION DISTINCT` instead of bare `UNION`. |
| `standard:final-newline` | Warning / Auto | Yes | Safe | Require files to end with one LF newline. |
| `standard:function-name-case` | Warning / Auto | Yes | Unsafe | Prefer uppercase common SQL function names outside comments and quoted text. |
| `standard:keyword-case` | Warning / Auto | Yes | Unsafe | Prefer uppercase common SQL keywords outside comments and quoted text. |
| `standard:line-ending-lf` | Warning / Auto | Yes | Safe | Replace CRLF or CR line endings with LF. |
| `standard:literal-case` | Warning / Auto | Yes | Unsafe | Prefer uppercase `NULL`, `TRUE`, and `FALSE` literals. |
| `standard:max-blank-lines` | Warning / Auto | Yes | Safe | Disallow more than one consecutive blank line. |
| `standard:max-case-depth` | Warning / Auto | No | None | Disallow `CASE` expressions nested deeper than `maxDepth`. |
| `standard:max-joins` | Warning / Auto | No | None | Disallow statements with more than `max` `JOIN` clauses. |
| `standard:max-line-length` | Warning / Auto | No | None | Report non-blank lines longer than 120 characters. |
| `standard:max-subquery-depth` | Warning / Auto | No | None | Disallow nested `SELECT` statements deeper than `maxDepth`. |
| `standard:no-consecutive-semicolons` | Warning / Auto | Yes | Safe | Disallow directly repeated semicolon tokens. |
| `standard:no-delete-without-where` | Warning / Auto | No | None | Disallow `DELETE` statements without a top-level `WHERE`. |
| `standard:no-distinct-parentheses` | Warning / Auto | Yes | Safe | Disallow parentheses immediately after `SELECT DISTINCT`. |
| `standard:no-else-null` | Warning / Auto | No | None | Disallow redundant `ELSE NULL` branches in `CASE` expressions. |
| `standard:no-from-subquery` | Warning / Auto | No | None | Prefer CTEs over top-level `FROM (SELECT ...)` and `JOIN (SELECT ...)` subqueries. |
| `standard:no-leading-blank-lines` | Warning / Auto | Yes | Safe | Disallow blank lines before the first content line. |
| `standard:no-leading-comma` | Warning / Auto | No | None | Disallow comma tokens as the first non-whitespace character on a line. |
| `standard:no-leading-whitespace` | Warning / Auto | Yes | Safe | Disallow any whitespace before the first file content. |
| `standard:no-leading-wildcard-like` | Warning / Auto | No | None | Disallow `LIKE` patterns that start with `%` or `_`. |
| `standard:no-redundant-semicolons` | Warning / Auto | Yes | Safe | Disallow repeated semicolons separated only by whitespace. |
| `standard:no-space-after-dot` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately after `.`. |
| `standard:no-space-after-opening-parenthesis` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately after `(`. |
| `standard:no-space-before-closing-parenthesis` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately before `)`. |
| `standard:no-space-before-comma` | Warning / Auto | Yes | Safe | Disallow inline whitespace before `,`. |
| `standard:no-space-before-dot` | Warning / Auto | Yes | Safe | Disallow inline whitespace immediately before `.`. |
| `standard:no-space-before-function-parenthesis` | Warning / Auto | Yes | Safe | Disallow inline whitespace between common SQL function names and `(`. |
| `standard:no-space-before-semicolon` | Warning / Auto | Yes | Safe | Disallow inline whitespace before `;`. |
| `standard:no-right-join` | Warning / Auto | No | None | Prefer writing joins as `LEFT JOIN` instead of `RIGHT JOIN`. |
| `standard:no-select-distinct-with-group-by` | Warning / Auto | No | None | Disallow `SELECT DISTINCT` and `GROUP BY` in the same statement. |
| `standard:no-select-star` | Warning / Auto | No | None | Disallow `SELECT *` result columns. |
| `standard:no-select-trailing-comma` | Warning / Auto | Yes | Unsafe | Disallow trailing commas at the end of `SELECT` clauses. |
| `standard:no-self-column-alias` | Warning / Auto | No | None | Disallow SELECT result aliases that repeat the source column name. |
| `standard:no-self-alias` | Warning / Auto | No | None | Disallow table aliases that repeat the table name they alias. |
| `standard:no-special-character-identifiers` | Warning / Auto | No | None | Disallow quoted identifiers that need non-portable special characters. |
| `standard:no-tab-indentation` | Warning / Auto | Yes | Safe | Replace leading indentation tabs with spaces. |
| `standard:no-trailing-blank-lines` | Warning / Auto | Yes | Safe | Disallow blank lines after the last content line. |
| `standard:no-trailing-whitespace` | Warning / Auto | Yes | Safe | Remove spaces or tabs at line ends. |
| `standard:no-unused-cte` | Warning / Auto | No | None | Disallow CTEs that are not referenced by the main query. |
| `standard:no-unused-join` | Warning / Auto | No | None | Disallow JOIN sources that are not referenced by later qualified column reads. |
| `standard:no-unnecessary-statement-parentheses` | Warning / Auto | No | None | Disallow redundant parentheses around whole top-level `SELECT` statements. |
| `standard:no-update-without-where` | Warning / Auto | No | None | Disallow `UPDATE` statements without a top-level `WHERE`. |
| `standard:operator-line-position` | Warning / Auto | No | None | Require multiline comparison and binary operators to trail the previous line. |
| `standard:prefer-coalesce` | Warning / Auto | Yes | Unsafe | Prefer `COALESCE` over `IFNULL` and `NVL`. |
| `standard:prefer-count-star` | Warning / Auto | Yes | Unsafe | Prefer `COUNT(*)` for row counts instead of `COUNT(1)` or `COUNT(0)`. |
| `standard:prefer-explicit-column-list-in-insert` | Warning / Auto | No | None | Require explicit target columns in `INSERT` statements. |
| `standard:prefer-simple-boolean-case` | Warning / Auto | No | None | Prefer direct boolean predicates over simple `CASE` expressions returning `TRUE` and `FALSE`. |
| `standard:require-column-alias-as` | Warning / Auto | No | None | Require `AS` for SELECT result column aliases. |
| `standard:require-order-by-with-limit` | Warning / Auto | No | None | Require `ORDER BY` when top-level `SELECT` statements use `LIMIT` or `OFFSET`. |
| `standard:require-result-column-alias` | Warning / Auto | No | None | Require aliases for computed `SELECT` result columns. |
| `standard:require-table-alias-as` | Warning / Auto | No | None | Require `AS` for table aliases. |
| `standard:require-table-alias-for-subquery` | Warning / Auto | No | None | Require aliases for top-level `FROM (SELECT ...)` and `JOIN (SELECT ...)` subqueries. |
| `standard:select-modifier-line-position` | Warning / Auto | No | None | Require `SELECT DISTINCT` and `SELECT ALL` modifiers to stay on the `SELECT` line. |
| `standard:set-operator-line-position` | Warning / Auto | No | None | Require multiline set operators to begin their own line after indentation. |
| `standard:space-after-block-comment-start` | Warning / Auto | Yes | Safe | Require one space after a block comment opening marker. |
| `standard:space-after-comma` | Warning / Auto | Yes | Safe | Require one inline space after `,` when another token follows. |
| `standard:space-after-line-comment-marker` | Warning / Auto | Yes | Safe | Require one space after `--` when comment text follows. |
| `standard:space-around-binary-operators` | Warning / Auto | Yes | Unsafe | Prefer one inline space around binary arithmetic and concatenation operators. |
| `standard:space-around-comparison-operators` | Warning / Auto | Yes | Unsafe | Prefer one inline space around comparison operators. |
| `standard:space-before-block-comment-end` | Warning / Auto | Yes | Safe | Require one space before a block comment closing marker. |
| `standard:statement-terminator` | Warning / Auto | No | None | Require statement blocks to end with semicolons. |
| `standard:unique-column-aliases` | Warning / Auto | No | None | Require SELECT result column aliases to be unique within a SELECT list. |
| `standard:unique-table-aliases` | Warning / Auto | No | None | Require top-level table aliases to be unique within a statement. |
| `standard:use-is-null` | Warning / Auto | Yes | Unsafe | Prefer `IS NULL` and `IS NOT NULL` over equality comparisons to `NULL`. |

## SQLFluff References

The initial standard rules are intentionally modeled after common sqlfluff layout, capitalisation, and structure rules,
but they are not a compatibility layer for sqlfluff rule codes. sqldelight-check keeps its own IDs because the rule
engine runs in SQLDelight projects, reports SQLDelight database metadata, and needs a stable API for custom rule sets.

Useful sqlfluff concepts reflected here:

- Capitalisation checks: `standard:keyword-case`, `standard:function-name-case`, `standard:data-type-case`, and
  `standard:literal-case`.
- Layout checks around dots, commas, semicolons, parentheses, function calls, comments, operators, blank lines, and line
  endings: the `standard:no-*` and `standard:space-*` spacing rules.
- Structure checks for repeated semicolons: `standard:no-consecutive-semicolons` and
  `standard:no-redundant-semicolons`.
- Convention checks for row counts, `NULL` comparisons, and join direction: `standard:prefer-count-star`,
  `standard:use-is-null`, and `standard:no-right-join`.
- Convention and ambiguity checks for operator, `CASE`, and set-operation clarity:
  `standard:consistent-not-equal-operator`, `standard:prefer-coalesce`, `standard:no-else-null`,
  `standard:prefer-simple-boolean-case`, `standard:explicit-union-operator`, `standard:operator-line-position`, and
  `standard:set-operator-line-position`.
- Ambiguity checks for `SELECT` and `ORDER BY`: `standard:no-select-distinct-with-group-by`,
  `standard:consistent-order-by-direction`, `standard:consistent-column-references`,
  `standard:require-order-by-with-limit`, `standard:explicit-cross-join`, `standard:explicit-inner-join`,
  `standard:no-distinct-parentheses`, `standard:no-select-trailing-comma`,
  `standard:select-modifier-line-position`, `standard:clause-keyword-newline`,
  `standard:no-unnecessary-statement-parentheses`, `standard:no-from-subquery`, and
  `standard:consistent-set-operation-column-count`.
- Parse-light convention checks for blocked words and table aliases: `standard:blocked-words`,
  `standard:no-self-alias`, `standard:require-table-alias-for-subquery`, and
  `standard:unique-table-aliases`.
- Parse-light alias and identifier checks from SQLFluff AL01, AL02, AL08, and RF05:
  `standard:require-table-alias-as`, `standard:require-column-alias-as`,
  `standard:unique-column-aliases`, `standard:no-self-column-alias`, and
  `standard:no-special-character-identifiers`.
- Parse-light reference checks from SQLFluff ST03 and ST11: `standard:no-unused-cte` and
  `standard:no-unused-join`.

## `standard:blocked-words`

Reports configured words that should not appear in SQL source.

Configure words with the comma-separated `words` option. Matching is case-insensitive by default. Set `matchCase=true`
to require exact-case matches. Comments are ignored by default; set `ignoreComments=false` to also scan line and block
comments.

Invalid with `words=deprecated`:

```sql
selectDeprecated:
SELECT deprecated
FROM player;
```

Valid:

```sql
selectPlayer:
SELECT id
FROM player;
```

Fix behavior:

- No fix is provided.
- String literals and quoted identifiers are ignored.

## `standard:no-self-alias`

Reports table aliases that repeat the table name they alias.

Invalid:

```sql
selectPlayers:
SELECT player.id
FROM player AS player;
```

Valid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p;
```

Fix behavior:

- No fix is provided.

## `standard:no-self-column-alias`

Reports SELECT result aliases that repeat the source column name.

Invalid:

```sql
selectPlayers:
SELECT name AS name
FROM player;
```

Invalid:

```sql
selectPlayers:
SELECT player.name AS name
FROM player;
```

Valid:

```sql
selectPlayers:
SELECT name AS playerName
FROM player;
```

Fix behavior:

- No fix is provided.
- Computed expressions are ignored because their alias can intentionally match an output concept.
- Quoted identifiers are ignored because quoting semantics are dialect-specific.

## `standard:no-special-character-identifiers`

Reports quoted identifiers that contain characters outside letters, digits, and `_`.

Invalid:

```sql
CREATE TABLE "player score" (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Valid:

```sql
CREATE TABLE player_score (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Fix behavior:

- No fix is provided.
- Single-quoted string literals and comments are ignored.

## `standard:require-column-alias-as`

Reports SELECT result aliases that omit `AS`.

Invalid:

```sql
selectStats:
SELECT count(*) total
FROM player;
```

Valid:

```sql
selectStats:
SELECT count(*) AS total
FROM player;
```

Fix behavior:

- No fix is provided.

## `standard:no-unused-cte`

Reports `WITH name AS (...)` CTEs that are not referenced by the main query.

Invalid:

```sql
selectPlayers:
WITH ranked AS (
  SELECT id
  FROM player
)
SELECT id
FROM player;
```

Valid:

```sql
selectPlayers:
WITH ranked AS (
  SELECT id
  FROM player
)
SELECT id
FROM ranked;
```

Fix behavior:

- No fix is provided.
- Recursive or ambiguous CTE layouts are left to future SQLDelight-derived facts.

## `standard:no-unused-join`

Reports JOIN sources whose alias or table name is not used as a later qualified column reference.

Invalid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p
JOIN team AS t ON p.team_id IS NOT NULL;
```

Valid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p
JOIN team AS t ON t.id = p.team_id;
```

Fix behavior:

- No fix is provided.
- The rule ignores comments and string literals when checking for qualified references.

## `standard:require-table-alias-as`

Reports table aliases that omit `AS`.

Invalid:

```sql
selectPlayers:
SELECT p.id
FROM player p;
```

Valid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p;
```

Fix behavior:

- No fix is provided.

## `standard:require-table-alias-for-subquery`

Reports top-level `FROM` and `JOIN` subqueries that do not declare a table alias.

Invalid:

```sql
selectPlayers:
SELECT id
FROM (SELECT id FROM player);
```

Valid:

```sql
selectPlayers:
SELECT ranked.id
FROM (SELECT id FROM player) AS ranked;
```

Fix behavior:

- No fix is provided.
- Nested subqueries are ignored until SQLDelight-derived parse facts are available.

## `standard:unique-table-aliases`

Reports duplicate top-level table aliases within the same statement.

Invalid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p
JOIN team AS p ON p.id = player.team_id;
```

Valid:

```sql
selectPlayers:
SELECT p.id
FROM player AS p
JOIN team AS t ON t.id = p.team_id;
```

Fix behavior:

- No fix is provided.
- Nested aliases are not compared with outer statement aliases.

## `standard:unique-column-aliases`

Reports duplicate aliases within the same SELECT list.

Invalid:

```sql
selectStats:
SELECT count(*) AS total, max(score) AS total
FROM player;
```

Valid:

```sql
selectStats:
SELECT count(*) AS total, max(score) AS max_score
FROM player;
```

Fix behavior:

- No fix is provided.
- Nested SELECT lists are checked independently from outer SELECT lists.

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

## `standard:no-leading-comma`

Reports comma tokens in multiline SQL when the comma is the first non-whitespace character on a line.

This rule is inspired by sqlfluff's leading-comma layout checks. The standard style keeps commas trailing, matching the
other comma spacing rules in this rule set.

Invalid:

```sql
selectPlayers:
SELECT id
  , name
FROM player;
```

Valid:

```sql
selectPlayers:
SELECT id,
  name
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Single-line SQL is accepted.
- Skips comments, string literals, and quoted identifiers.

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

## `standard:no-space-before-dot`

Reports spaces and tabs immediately before `.` outside comments and quoted text.

Qualified column references are common in SQLDelight query files when joins are introduced. Keeping the dot tight on
both sides prevents `table . column` variants from drifting through a codebase.

Invalid:

```sql
selectPlayer:
SELECT player .id
FROM player;
```

Valid:

```sql
selectPlayer:
SELECT player.id
FROM player;
```

Fix behavior:

- Removes inline spaces and tabs immediately before `.`.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:no-space-after-dot`

Reports spaces and tabs immediately after `.` outside comments and quoted text.

Invalid:

```sql
selectPlayer:
SELECT player. id
FROM player;
```

Valid:

```sql
selectPlayer:
SELECT player.id
FROM player;
```

Fix behavior:

- Removes inline spaces and tabs immediately after `.`.
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

## `standard:no-space-before-function-parenthesis`

Reports spaces and tabs between a recognized SQL function name and the following `(`.

Function calls are easier to distinguish from grouping expressions when the function token is tight to its argument
list. The rule uses the same common function-name catalog as `standard:function-name-case`.

Invalid:

```sql
selectPlayerStats:
SELECT COUNT (*), COALESCE (MAX(score), 0)
FROM player;
```

Valid:

```sql
selectPlayerStats:
SELECT COUNT(*), COALESCE(MAX(score), 0)
FROM player;
```

Ignored:

```sql
selectWrapped:
SELECT *
FROM (SELECT id FROM player) AS nested_player;
```

Fix behavior:

- Removes inline spaces and tabs between a recognized function token and `(`.
- Skips grouping parentheses that are not preceded by a recognized function name.
- Skips comments, string literals, and quoted identifiers.
- Safe to apply in write tasks.

## `standard:space-around-binary-operators`

Reports binary arithmetic and concatenation operators that do not have exactly one inline space on both sides.

Covered operators:

```text
+, -, *, /, %, ||
```

Invalid:

```sql
updateScore:
UPDATE player
SET score=score+1
WHERE id = ?;
```

Valid:

```sql
updateScore:
UPDATE player
SET score=score + 1
WHERE id = ?;
```

Ignored:

```sql
selectConstants:
SELECT +2, -4, COUNT(*)
FROM player;
```

Fix behavior:

- Replaces surrounding inline spaces or tabs with one space on each side.
- Skips operators split across lines.
- Skips common unary plus/minus cases and `*` in `SELECT *` or `COUNT(*)`.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- Dialects and extensions can define operator behavior that is difficult to infer from source text alone.
- Unary and binary operator roles can be context-sensitive.
- The current rule uses source text rather than SQLDelight PSI, so users must opt in before write tasks apply fixes.

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

## `standard:operator-line-position`

Reports comparison and binary operators in multiline SQL when the operator is the first non-whitespace character on a
line.

This rule is inspired by sqlfluff's operator line-position rule. The standard style uses trailing operators because the
existing operator spacing rules keep operators between operands.

Invalid:

```sql
selectPlayers:
SELECT id
FROM player
WHERE score
  >= 10;
```

Valid:

```sql
selectPlayers:
SELECT id
FROM player
WHERE score >=
  10;
```

Fix behavior:

- No automatic fix is provided.
- Single-line SQL is accepted.
- Skips comments, string literals, and quoted identifiers.

## `standard:space-after-line-comment-marker`

Reports `--` line comments when comment text follows immediately without a space.

SQLDelight files often contain short comments above schema declarations, named queries, and migrations. Requiring
`-- comment` keeps those comments readable while leaving empty `--` separator comments alone.

Invalid:

```sql
--Player lookup queries.
selectAll:
SELECT id, name
FROM player;
```

Valid:

```sql
-- Player lookup queries.
selectAll:
SELECT id, name
FROM player;
```

Valid:

```sql
--
selectAll:
SELECT id, name
FROM player;
```

Fix behavior:

- Inserts one space after `--` when another character follows on the same line.
- Skips `--` sequences inside string literals and quoted identifiers.
- Safe to apply in write tasks.

## `standard:space-after-block-comment-start`

Reports block comments when comment text follows the opening marker immediately without a space.

SQLDelight migrations often use inline notes next to schema changes. Requiring `/* comment */` keeps those comments
readable while leaving empty block comments and hint-like `/*+ ... */` comments alone.

Invalid:

```sql
/*Player table.*/
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Valid:

```sql
/* Player table. */
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Ignored:

```sql
/**/
/*+ dialect-specific hint-like comment */
```

Fix behavior:

- Inserts one space after the opening marker when comment text follows immediately.
- Skips empty block comments and `/*+` comments.
- Skips block comment markers inside string literals and quoted identifiers.
- Safe to apply in write tasks.

## `standard:space-before-block-comment-end`

Reports block comments when comment text touches the closing marker without a preceding space.

Invalid:

```sql
/* Player table.*/
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Valid:

```sql
/* Player table. */
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY
);
```

Fix behavior:

- Inserts one space before the closing marker when comment text touches it.
- Skips empty block comments.
- Skips block comment markers inside string literals and quoted identifiers.
- Safe to apply in write tasks.

## `standard:function-name-case`

Reports common SQL function names that are not uppercase.

This rule is inspired by sqlfluff's function capitalisation rule. sqldelight-check only checks recognized function-name
tokens followed by `(`, so function-like column names are left alone.

Invalid:

```sql
selectPlayerStats:
SELECT count(*), coalesce(max(score), 0)
FROM player;
```

Valid:

```sql
selectPlayerStats:
SELECT COUNT(*), COALESCE(MAX(score), 0)
FROM player;
```

Fix behavior:

- Replaces recognized function tokens with uppercase text.
- Marks fixes as `Unsafe`.
- Skips comments, string literals, and quoted identifiers.

Why unsafe:

- Function names can be dialect-specific.
- User-defined functions can use project-specific naming conventions.
- The rule uses source text rather than SQLDelight PSI, so users must opt in before write tasks apply fixes.

Current function coverage:

```text
ABS, AVG, COALESCE, COUNT, DATE, DATETIME, GROUP_CONCAT, HEX, IFNULL, INSTR,
JSON_EXTRACT, LENGTH, LOWER, LTRIM, MAX, MIN, NULLIF, RANDOM, REPLACE, ROUND,
RTRIM, STRFTIME, SUBSTR, SUBSTRING, SUM, TIME, TRIM, TYPEOF, UPPER
```

## `standard:data-type-case`

Reports common SQL data type names that are not uppercase.

This rule is inspired by sqlfluff's data type capitalisation rule. It is intentionally conservative and only recognizes
common type names that appear in SQLDelight schema and migration files.

Invalid:

```sql
CREATE TABLE player (
  id integer NOT NULL PRIMARY KEY,
  name text NOT NULL
);
```

Valid:

```sql
CREATE TABLE player (
  id INTEGER NOT NULL PRIMARY KEY,
  name TEXT NOT NULL
);
```

Fix behavior:

- Replaces recognized data type tokens with uppercase text.
- Marks fixes as `Unsafe`.
- Skips comments, string literals, and quoted identifiers.

Why unsafe:

- SQLDelight supports dialects and custom column adapters.
- Some projects may intentionally use custom type names or type aliases.
- The rule uses source text rather than SQLDelight PSI, so users must opt in before write tasks apply fixes.

Current data type coverage:

```text
BIGINT, BLOB, BOOL, BOOLEAN, CHAR, CLOB, DECIMAL, DOUBLE, FLOAT, INT, INTEGER,
NUMERIC, REAL, SMALLINT, TEXT, TIMESTAMP, VARCHAR
```

## `standard:literal-case`

Reports SQL literal tokens that are not uppercase.

This rule is inspired by sqlfluff's literal capitalisation rule and owns literal casing separately from
`standard:keyword-case`.

Invalid:

```sql
selectActive:
SELECT id, name
FROM player
WHERE deleted_at IS null AND active = true;
```

Valid:

```sql
selectActive:
SELECT id, name
FROM player
WHERE deleted_at IS NULL AND active = TRUE;
```

Fix behavior:

- Replaces `null`, `true`, and `false` variants with uppercase text.
- Marks fixes as `Unsafe`.
- Skips comments, string literals, and quoted identifiers.

Why unsafe:

- Literal case is normally behavior-preserving, but without SQLDelight PSI the rule cannot prove every unquoted token is
  syntactically a literal.
- Users must opt in before write tasks apply fixes.

## `standard:prefer-coalesce`

Reports `IFNULL` and `NVL` calls.

This rule is inspired by sqlfluff's coalesce convention rule. `COALESCE` is the portable spelling and accepts more than
two arguments, so the standard rule set prefers it for SQLDelight projects that target multiple dialects.

Invalid:

```sql
selectDisplayName:
SELECT IFNULL(nickname, name)
FROM player;
```

Valid:

```sql
selectDisplayName:
SELECT COALESCE(nickname, name)
FROM player;
```

Fix behavior:

- Replaces the function token with `COALESCE`.
- Skips identifiers named `ifnull` or `nvl` when they are not followed by `(`.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- `IFNULL`, `NVL`, and `COALESCE` can differ in dialect support, type inference, and evaluation details.
- Users must opt in before write tasks apply fixes.

## `standard:prefer-count-star`

Reports `COUNT(1)` and `COUNT(0)` when they are used as row-counting syntax.

This rule is inspired by sqlfluff's row-count convention rule. The standard rule set defaults to `COUNT(*)` because it
is the explicit SQL spelling for counting rows.

Invalid:

```sql
selectPlayerCount:
SELECT COUNT(1)
FROM player;
```

Valid:

```sql
selectPlayerCount:
SELECT COUNT(*)
FROM player;
```

Fix behavior:

- Replaces the single `0` or `1` argument with `*`.
- Leaves `COUNT(column)`, `COUNT(DISTINCT column)`, and other aggregate expressions alone.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- `COUNT(1)` and `COUNT(*)` are equivalent for common row-counting use, but projects may still choose a house style or
  rely on dialect-specific explanation plans.
- Users must opt in before write tasks apply fixes.

## `standard:use-is-null`

Reports equality comparisons where `NULL` appears on the right-hand side.

SQL `NULL` semantics require `IS NULL` and `IS NOT NULL` for presence checks. This rule follows sqlfluff's convention
rule while staying conservative around assignment contexts.

Invalid:

```sql
selectMissingName:
SELECT id, name
FROM player
WHERE name = NULL;
```

Valid:

```sql
selectMissingName:
SELECT id, name
FROM player
WHERE name IS NULL;
```

Fix behavior:

- Replaces `=` with `IS`.
- Replaces `!=` and `<>` with `IS NOT`.
- Skips `SET` clause assignments such as `SET deleted_at = NULL`.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- The replacement intentionally changes SQL behavior from equality comparison semantics to `NULL` predicate semantics.
- The first implementation uses source text to avoid `SET` clause assignments, not SQLDelight expression facts.
- Users must opt in before write tasks apply fixes.

## `standard:consistent-not-equal-operator`

Reports files that mix `!=` and `<>` not-equal operators.

The first not-equal operator in a file defines the convention for that file. Later operators using the other spelling are
reported.

Invalid:

```sql
selectMismatchedPlayers:
SELECT id, name
FROM player
WHERE name != 'admin' AND status <> 'deleted';
```

Valid:

```sql
selectMismatchedPlayers:
SELECT id, name
FROM player
WHERE name != 'admin' AND status != 'deleted';
```

Fix behavior:

- Replaces later mismatched not-equal operators with the first not-equal operator spelling seen in the file.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- Not every dialect supports both not-equal spellings equally.
- Users must opt in before write tasks apply fixes.

## `standard:explicit-union-operator`

Reports `UNION` operators that do not explicitly specify `ALL` or `DISTINCT`.

This rule is inspired by sqlfluff's ambiguous union rule. Bare `UNION` has distinct semantics, but requiring
`UNION DISTINCT` makes that choice visible in code review.

Invalid:

```sql
selectAllNames:
SELECT name FROM active_player
UNION
SELECT name FROM archived_player;
```

Valid:

```sql
selectAllNames:
SELECT name FROM active_player
UNION DISTINCT
SELECT name FROM archived_player;
```

Fix behavior:

- No automatic fix is provided because the rule cannot know whether the intended set operation is `ALL` or `DISTINCT`.
- Skips comments, string literals, and quoted identifiers.

## `standard:set-operator-line-position`

Reports `UNION`, `EXCEPT`, and `INTERSECT` in multiline SQL when the set operator is not at the start of its own line
after indentation.

This rule is inspired by sqlfluff's set-operator line-position rule. Keeping set operators line-leading makes compound
queries easier to scan and diff.

Invalid:

```sql
selectAllNames:
SELECT name FROM active_player UNION
SELECT name FROM archived_player;
```

Valid:

```sql
selectAllNames:
SELECT name FROM active_player
UNION
SELECT name FROM archived_player;
```

Fix behavior:

- No automatic fix is provided.
- Single-line SQL is accepted.
- Skips comments, string literals, and quoted identifiers.

## `standard:no-select-distinct-with-group-by`

Reports statements that use both `SELECT DISTINCT` and `GROUP BY`.

This rule is inspired by sqlfluff's ambiguous distinct rule. `GROUP BY` already creates grouped output, so combining it
with `DISTINCT` is usually redundant or unclear.

Invalid:

```sql
selectDistinctNames:
SELECT DISTINCT name
FROM player
GROUP BY name;
```

Valid:

```sql
selectDistinctNames:
SELECT DISTINCT name
FROM player;
```

Valid:

```sql
selectGroupedNames:
SELECT name
FROM player
GROUP BY name;
```

Fix behavior:

- No automatic fix is provided because the rule cannot know whether `DISTINCT` or `GROUP BY` should be removed.
- Skips comments, string literals, and quoted identifiers.

## `standard:consistent-order-by-direction`

Reports `ORDER BY` clauses that mix explicit and implicit sort directions.

This rule is inspired by sqlfluff's ambiguous ordering rule. If one item specifies `ASC` or `DESC`, all items should do
so; otherwise all items can rely on the default direction.

Invalid:

```sql
selectPlayers:
SELECT id, name, score
FROM player
ORDER BY name, score DESC;
```

Valid:

```sql
selectPlayers:
SELECT id, name, score
FROM player
ORDER BY name ASC, score DESC;
```

Valid:

```sql
selectPlayers:
SELECT id, name, score
FROM player
ORDER BY name, score;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.

## `standard:consistent-set-operation-column-count`

Reports `UNION`, `INTERSECT`, and `EXCEPT` operations whose adjacent `SELECT` lists return different column counts.

Invalid:

```sql
selectAllPlayers:
SELECT id, name
FROM active_player
UNION ALL
SELECT id
FROM archived_player;
```

Valid:

```sql
selectAllPlayers:
SELECT id, name
FROM active_player
UNION ALL
SELECT id, name
FROM archived_player;
```

Fix behavior:

- No automatic fix is provided because the rule cannot know which branch should add or remove columns.
- Branches with wildcard targets are skipped because their result count is unknown.
- Skips comments, string literals, and quoted identifiers.

## `standard:no-select-trailing-comma`

Reports trailing commas at the end of a `SELECT` clause.

This rule is inspired by sqlfluff's select trailing comma convention rule. Some dialects accept trailing select-list
commas, but sqldelight-check's standard style forbids them by default.

Invalid:

```sql
selectPlayers:
SELECT id, name,
FROM player;
```

Valid:

```sql
selectPlayers:
SELECT id, name
FROM player;
```

Fix behavior:

- Removes the trailing comma before `FROM`.
- Skips comments, string literals, and quoted identifiers.
- Marks fixes as `Unsafe`.

Why unsafe:

- Dialects differ on whether trailing select-list commas are accepted.
- The first implementation uses source text rather than SQLDelight select-clause facts.
- Users must opt in before write tasks apply fixes.

## `standard:select-modifier-line-position`

Reports `SELECT DISTINCT` and `SELECT ALL` modifiers when the modifier is not on the same line as `SELECT`.

This rule is inspired by sqlfluff's `SELECT` modifier layout rule. Keeping the modifier next to `SELECT` makes the row
deduplication or all-row intent visible at the statement start.

Invalid:

```sql
selectNames:
SELECT
  DISTINCT name
FROM player;
```

Valid:

```sql
selectNames:
SELECT DISTINCT name
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.

## `standard:clause-keyword-newline`

Reports major top-level `SELECT` clause keywords in multiline statements when the keyword does not start its own line
after indentation.

This is a conservative source-text subset inspired by sqlfluff's clause newline rule. It checks top-level `SELECT`
statements and skips clauses inside parentheses.

Checked clauses:

```text
FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT, OFFSET
```

Invalid:

```sql
selectPlayers:
SELECT id FROM player WHERE score > 0
ORDER BY id LIMIT 10;
```

Valid:

```sql
selectPlayers:
SELECT id
FROM player
WHERE score > 0
ORDER BY id
LIMIT 10;
```

Fix behavior:

- No automatic fix is provided.
- Single-line SQL is accepted.
- Skips comments, string literals, quoted identifiers, and nested parenthesized clauses.

## `standard:no-right-join`

Reports `RIGHT JOIN` and `RIGHT OUTER JOIN`.

This rule is inspired by sqlfluff's left-join convention rule. It has no automatic fix because rewriting the join safely
requires swapping relation order and preserving join predicates.

Invalid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM player
RIGHT JOIN team ON team.id = player.team_id;
```

Valid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM team
LEFT JOIN player ON team.id = player.team_id;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.

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
ON, OR, ORDER, OUTER, PRIMARY, REFERENCES, RIGHT, SELECT, SET, TABLE, THEN,
UNION, UNIQUE, UPDATE, VALUES, WHEN, WHERE
```

## `standard:max-line-length`

Reports non-blank lines longer than 120 characters.

This rule is inspired by sqlfluff's long-line layout rule. The first implementation is lint-only: it reports the
overflow range but does not attempt to reflow SQL, comments, or string literals.

Invalid:

```sql
selectPlayers:
SELECT id, name, email, phone_number, address_line_1, address_line_2, city, region, postal_code, country, created_at, updated_at FROM player;
```

Valid:

```sql
selectPlayers:
SELECT id, name, email
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Blank lines and whitespace-only lines are ignored.
- Comments and string literals count toward the line length because the rule checks source text length.

## `standard:no-leading-whitespace`

Reports files that start with spaces, tabs, CR, or LF before the first content character.

This is stricter than `standard:no-leading-blank-lines`: it also catches indentation before the first SQLDelight import,
schema statement, query label, or migration statement.

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

- Removes the leading whitespace range.
- Marks fixes as `Safe`.
- Leaves empty files unchanged.

## `standard:statement-terminator`

Reports SQLDelight statement blocks that do not end with `;`.

For `.sqm` files, the rule checks migration statements. For `.sq` files, the rule uses SQLDelight labels and conservative
statement starts to avoid treating import lines or text inside strings as SQL statements.

Invalid:

```sql
selectAll:
SELECT id, name
FROM player
ORDER BY name
```

Valid:

```sql
selectAll:
SELECT id, name
FROM player
ORDER BY name;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.
- Skips trigger bodies until SQLDelight parser-backed statement ranges are exposed to rules.

## `standard:no-redundant-semicolons`

Reports repeated semicolon tokens separated only by whitespace.

This extends `standard:no-consecutive-semicolons`: it also catches cases such as `; ;` and a semicolon repeated on the
next line.

Invalid:

```sql
selectAll:
SELECT id
FROM player;
;
```

Valid:

```sql
selectAll:
SELECT id
FROM player;
```

Fix behavior:

- Keeps the first semicolon and removes the redundant semicolons plus safe surrounding whitespace.
- Marks fixes as `Safe`.
- Skips comments, string literals, and quoted identifiers.

## `standard:consistent-column-references`

Reports `GROUP BY` and `ORDER BY` clauses that mix ordinal references with named or expression references.

Ordinal references such as `GROUP BY 1` are concise but become hard to read when mixed with names. The rule reports the
clause header and leaves the rewrite to the user.

Invalid:

```sql
selectScores:
SELECT team_id, name, SUM(score)
FROM player
GROUP BY 1, name;
```

Valid:

```sql
selectScores:
SELECT team_id, name, SUM(score)
FROM player
GROUP BY team_id, name;
```

Fix behavior:

- No automatic fix is provided.
- `ORDER BY 1 DESC` and `ORDER BY 1 NULLS LAST` are treated as ordinal references.
- Parenthesized `ORDER BY` clauses, such as window clauses, are skipped by the source-text implementation.

## `standard:require-order-by-with-limit`

Reports top-level `SELECT` statements that use `LIMIT` or `OFFSET` without `ORDER BY`.

Rows returned by a limited query are not stable unless the query defines an ordering. The rule checks top-level SELECT
statements and ignores nested subqueries while the rule model remains source-text based.

Invalid:

```sql
selectPlayers:
SELECT id, name
FROM player
LIMIT 10;
```

Valid:

```sql
selectPlayers:
SELECT id, name
FROM player
ORDER BY name
LIMIT 10;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, quoted identifiers, and parenthesized nested query text.
- Compound SELECT and dialect-specific limit syntax may need parser-backed refinement.

## `standard:explicit-cross-join`

Reports `JOIN` clauses that do not provide `ON` or `USING` and are not explicitly written as `CROSS JOIN` or
`NATURAL JOIN`.

The rule makes intentional cartesian joins visible. It does not rewrite joins automatically because changing join type
syntax can affect readability and dialect behavior.

Invalid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM player
JOIN team;
```

Valid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM player
CROSS JOIN team;
```

Fix behavior:

- No automatic fix is provided.
- `JOIN ... ON ...` and `JOIN ... USING (...)` are accepted.
- Complex nested join syntax is handled conservatively until parser-backed join ranges are exposed.

## `standard:explicit-inner-join`

Reports bare `JOIN` clauses that provide `ON` or `USING` instead of explicitly writing `INNER JOIN`.

The rule keeps conditioned joins explicit while leaving outer, cross, and natural join syntax alone.

Invalid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM player
JOIN team ON team.id = player.team_id;
```

Valid:

```sql
selectPlayerTeams:
SELECT player.id, team.name
FROM player
INNER JOIN team ON team.id = player.team_id;
```

Fix behavior:

- No automatic fix is provided.
- `LEFT`, `RIGHT`, `FULL`, `CROSS`, `NATURAL`, and `INNER` joins are accepted.
- Complex nested join syntax is handled conservatively until parser-backed join ranges are exposed.

## `standard:no-distinct-parentheses`

Reports `SELECT DISTINCT(...)` syntax.

`DISTINCT` applies to the selected row, not to a function argument. Removing the parentheses makes the SQL intent match
the syntax more directly.

Invalid:

```sql
selectNames:
SELECT DISTINCT(name)
FROM player;
```

Valid:

```sql
selectNames:
SELECT DISTINCT name
FROM player;
```

Fix behavior:

- Removes parentheses for simple identifiers, dotted identifiers, and `*`.
- Marks simple fixes as `Safe`.
- Reports complex or multiline expressions without a fix.

## `standard:no-else-null`

Reports `CASE` expressions with an explicit `ELSE NULL` branch.

`CASE` already returns `NULL` when no branch matches and no `ELSE` branch is provided, so the explicit null branch is
usually redundant.

Invalid:

```sql
selectPlayerStatus:
SELECT CASE
  WHEN score > 10 THEN 'starter'
  ELSE NULL
END AS status
FROM player;
```

Valid:

```sql
selectPlayerStatus:
SELECT CASE
  WHEN score > 10 THEN 'starter'
END AS status
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.

## `standard:no-unnecessary-statement-parentheses`

Reports redundant parentheses around a whole top-level `SELECT` statement.

This is a conservative source-text subset of SQLFluff CV07. It only reports statement-level shapes such as a SQLDelight
query body or migration statement written as `(SELECT ...);`; expression parentheses and subquery parentheses are left
alone.

Invalid:

```sql
selectPlayers:
(SELECT id, name
FROM player);
```

Valid:

```sql
selectPlayers:
SELECT id, name
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, and quoted identifiers.
- Skips compound or ambiguous parenthesized constructs.

## `standard:prefer-simple-boolean-case`

Reports `CASE WHEN predicate THEN TRUE ELSE FALSE END` and the inverse
`CASE WHEN predicate THEN FALSE ELSE TRUE END`.

These expressions can often be written as the predicate or its negation, but no fix is provided because SQL
three-valued logic can make an automatic rewrite change `NULL` behavior.

Invalid:

```sql
selectActive:
SELECT CASE WHEN score > 10 THEN TRUE ELSE FALSE END AS active
FROM player;
```

Valid:

```sql
selectActive:
SELECT score > 10 AS active
FROM player;
```

Fix behavior:

- No automatic fix is provided.
- Reports only exact single-branch boolean literal cases.
- Skips comments, string literals, and quoted identifiers.

## `standard:no-from-subquery`

Reports top-level `FROM (SELECT ...)` and `JOIN (SELECT ...)` subqueries.

This is a conservative source-text subset of SQLFluff ST05. It encourages moving the subquery to a CTE when the
subquery is a direct table expression in the top-level `FROM` or `JOIN` list.

Invalid:

```sql
selectPlayers:
SELECT ranked.id
FROM (SELECT id FROM player) AS ranked;
```

Valid:

```sql
selectPlayers:
WITH ranked AS (
  SELECT id
  FROM player
)
SELECT ranked.id
FROM ranked;
```

Fix behavior:

- No automatic fix is provided.
- Skips comments, string literals, quoted identifiers, and nested subqueries.
- Skips ambiguous constructs until parser-backed table-expression ranges are exposed.

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
