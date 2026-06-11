# Changelog

## v0.1.0

Initial release of sqldelight-check.

This version establishes the first Gradle plugin and API surface for checking SQLDelight `.sq` and `.sqm` files.

### Initial Scope

- Added Gradle plugin `dev.s7a.sqldelight.check`.
- Added tasks for check, lint, format, and write variants.
- Added SQLDelight project detection for Gradle projects, including multiple databases.
- Added built-in SQLDelight `2.x` analysis for stable `2.0.x` through `2.3.x` releases, with opt-in compatibility
  verification for `2.4.0-SNAPSHOT`.
- Added best-effort file and source range mapping for SQLDelight parser/compiler diagnostics.
- Added stable API modules for diagnostics, rule sets, and reporters.
- Added built-in rule sets for standard, PostgreSQL, MySQL, SQLite, and HSQL rule families. See the rule set READMEs,
  including [the standard rule set README](rules/standard/README.md), for the maintained rule lists and examples.
- Added safe fix application for write tasks.
- Added JSON, SARIF, text, navigable HTML, GitHub Actions annotations, and GitHub Actions-friendly Markdown reporters.
- Added reporter options in the Gradle DSL; JSON and SARIF support `prettyPrint`.
- Added external provider discovery through `sqldelightCheckRuleSet` and `sqldelightCheckReporter`.
- Added Qodana configuration and Gradle TestKit coverage for the plugin.

### Notes

sqldelight-check is pre-1.0. API names and report schemas may change while the first real-world usage is incorporated.
