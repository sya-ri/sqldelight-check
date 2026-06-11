# Changelog

## v0.1.0

Initial release of sqldelight-check.

This version establishes the first Gradle plugin and API surface for checking SQLDelight `.sq` and `.sqm` files.

### Initial Scope

- Added Gradle plugin `dev.s7a.sqldelight.check`.
- Added `sqldelightCheck` and `sqldelightFix` tasks.
- Added SQLDelight Gradle project detection for `.sq` and `.sqm` files, including multiple databases and nested
  projects with configurable report roots.
- Added SQLDelight `2.x` Gradle model support for stable `2.0.x` through `2.3.x` releases, with opt-in compatibility
  checks for `2.4.0-SNAPSHOT`.
- Added source-text rule analysis for SQLDelight files. sqldelight-check does not replace SQLDelight parser or compiler
  diagnostics.
- Added API modules for shared models, rule authoring, reporter authoring, and SQLDelight dialect metadata providers.
- Added built-in standard, PostgreSQL, MySQL, SQLite, and HSQL rule sets. The Gradle plugin installs the official rule
  sets by default and gates dialect-specific rules by detected dialect capabilities.
- Added standard SQLDelight source-shape rules for import ordering, duplicate imports, wildcard imports, duplicate
  query labels, query label operation matching, mapped type name casing, parameter/column name matching, result alias
  casing, duplicate result-name aliases, grouped statement count limits, view naming, and `SELECT *` in views.
- Added per-rule and per-rule-set enablement and severity overrides, including database-specific overrides.
- Added inline disable directives with optional reason enforcement.
- Added safe fix application through `sqldelightFix`; unsafe fixes require explicit opt-in.
- Added Gradle diagnostic logging for rule hits at `info`, `warning`, and `error` severity levels.
- Added JSON, SARIF, text, navigable HTML, Markdown, and GitHub Actions annotations reporters.
- Added reporter options in the Gradle DSL, including `prettyPrint` for JSON and SARIF and configurable output files.
- Added external provider discovery through `sqldelightCheckRuleSet`, `sqldelightCheckReporter`, and
  `sqldelightCheckDialects`.
- Added Qodana configuration and Gradle TestKit coverage for the plugin.

### Notes

sqldelight-check is pre-1.0. API names and report schemas may change while the first real-world usage is incorporated.
There is no standalone CLI in this release.
