# Rule Rationale

This document records why built-in rules belong in sqldelight-check. It is a
release checklist for avoiding rules that are only personal preference.

## Admission Criteria

A built-in rule should satisfy at least one of these criteria:

- Source hygiene: prevents invisible or mechanically noisy source changes.
- Deterministic formatting: keeps equivalent SQL text stable across editors and
  generated diffs.
- SQL clarity: removes syntax choices that are easy to misread in reviews.
- Query safety: flags SQL that can cause broad data changes or unstable results.
- Maintainability: limits query shapes that are hard to review or evolve.
- SQLDelight portability: avoids source forms that are awkward for generated
  APIs or multiple SQLDelight dialects.
- Dialect migration safety: catches dialect-specific migration patterns that are
  risky for live databases.

Rules outside those criteria should stay out of the default built-in sets until
they have a clearer product reason, a dialect reason, or a project-configurable
option that makes the trade-off explicit.

## Severity Policy

Built-in rules default to `Severity.Warning` for `v0.1.0`. That keeps the first
release conservative while still surfacing diagnostics by default.

Users can promote or demote rules with `Severity.Error` and `Severity.Info` in
`build.gradle.kts`. SQLDelight parser and compiler diagnostics remain errors
because they come from SQLDelight itself.

## Standard Rule Audit

Source hygiene:

- `standard:final-newline`
- `standard:line-ending-lf`
- `standard:max-blank-lines`
- `standard:no-consecutive-semicolons`
- `standard:no-leading-blank-lines`
- `standard:no-leading-whitespace`
- `standard:no-redundant-semicolons`
- `standard:no-tab-indentation`
- `standard:no-trailing-blank-lines`
- `standard:no-trailing-whitespace`

Deterministic formatting:

- `standard:blank-line-between-statements`
- `standard:clause-keyword-newline`
- `standard:data-type-case`
- `standard:function-name-case`
- `standard:keyword-case`
- `standard:literal-case`
- `standard:no-distinct-parentheses`
- `standard:no-blank-line-after-query-label`
- `standard:no-leading-comma`
- `standard:no-select-trailing-comma`
- `standard:no-space-after-dot`
- `standard:no-space-after-opening-parenthesis`
- `standard:no-space-before-closing-parenthesis`
- `standard:no-space-before-comma`
- `standard:no-space-before-dot`
- `standard:no-space-before-function-parenthesis`
- `standard:no-space-before-semicolon`
- `standard:operator-line-position`
- `standard:select-comma-line-position`
- `standard:select-modifier-line-position`
- `standard:select-target-newline`
- `standard:set-operator-line-position`
- `standard:space-after-block-comment-start`
- `standard:space-after-comma`
- `standard:space-after-line-comment-marker`
- `standard:space-around-binary-operators`
- `standard:space-around-comparison-operators`
- `standard:space-before-block-comment-end`
- `standard:statement-terminator`

SQL clarity:

- `standard:consistent-column-references`
- `standard:consistent-not-equal-operator`
- `standard:consistent-order-by-direction`
- `standard:consistent-reference-qualification`
- `standard:explicit-cross-join`
- `standard:explicit-inner-join`
- `standard:explicit-union-operator`
- `standard:no-else-null`
- `standard:no-self-alias`
- `standard:no-self-column-alias`
- `standard:no-unnecessary-statement-parentheses`
- `standard:prefer-coalesce`
- `standard:prefer-count-star`
- `standard:prefer-simple-boolean-case`
- `standard:require-column-alias-as`
- `standard:require-parentheses-for-mixed-boolean-operators`
- `standard:require-result-column-alias`
- `standard:require-table-alias-as`
- `standard:require-table-alias-for-subquery`
- `standard:unique-column-aliases`
- `standard:unique-table-aliases`
- `standard:use-is-null`

Query safety:

- `standard:no-delete-without-where`
- `standard:no-leading-wildcard-like`
- `standard:no-select-distinct-with-group-by`
- `standard:no-select-star`
- `standard:no-update-without-where`
- `standard:require-order-by-with-limit`

Maintainability:

- `standard:blocked-words`
- `standard:consistent-set-operation-column-count`
- `standard:max-case-depth`
- `standard:max-joins`
- `standard:max-line-length`
- `standard:max-subquery-depth`
- `standard:no-from-subquery`
- `standard:no-unused-cte`
- `standard:no-unused-join`
- `standard:no-unknown-qualifier`
- `standard:prefer-explicit-column-list-in-insert`

SQLDelight portability:

- `standard:no-right-join`
- `standard:no-special-character-identifiers`

## Dialect Rule Audit

PostgreSQL migration safety:

- `postgres:excessive-locks`
- `postgres:require-concurrent-index`
- `postgres:no-concurrent-index-in-transaction`
- `postgres:require-not-valid-constraint`
- `postgres:no-set-not-null-on-existing-column`
- `postgres:no-add-column-with-volatile-default`
- `postgres:prefer-identity-over-serial`
- `postgres:no-drop-column`
- `postgres:no-rename-column`
- `postgres:no-rename-table`
- `postgres:reindex-concurrently`
- `postgres:risky-alter-table`

MySQL migration safety:

- `mysql:no-utf8-charset`
- `mysql:no-copy-algorithm`
- `mysql:no-exclusive-lock`
- `mysql:no-replace-into`
- `mysql:no-zero-date-default`
- `mysql:no-display-width-integer`
- `mysql:require-index-prefix-length`
- `mysql:risky-alter-table`

SQLite schema and migration safety:

- `sqlite:consistent-conflict-resolution`
- `sqlite:foreign-keys-restored`
- `sqlite:prefer-integer-primary-key`
- `sqlite:no-autoincrement-without-need`
- `sqlite:no-alter-table-complex-change`
- `sqlite:prefer-without-rowid-for-composite-pk`

HSQL source safety:

- `hsql:no-database-file-settings`
- `hsql:no-system-operations`
- `hsql:no-text-table-source`

## Audit Result

Every current built-in rule maps to at least one admission criterion. The rules
that are most project-dependent are `standard:blocked-words`,
`standard:max-line-length`, `standard:max-joins`,
`standard:max-subquery-depth`, and `standard:max-case-depth`; those are kept
because they are explicitly configurable.
