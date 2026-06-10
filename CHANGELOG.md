# Changelog

## v0.1.0

Initial release of sqldelight-check.

This version establishes the first Gradle plugin and API surface for checking SQLDelight `.sq` and `.sqm` files.

### Initial Scope

- Added Gradle plugin `dev.s7a.sqldelight.check`.
- Added tasks for check, lint, format, and write variants.
- Added SQLDelight project detection for Gradle projects, including multiple databases.
- Added a SQLDelight `2.3.2` adapter that delegates analysis to SQLDelight compiler APIs.
- Added stable API modules for diagnostics, rule sets, reporters, and SQLDelight adapters.
- Added standard rules:
  - `standard:final-newline`
  - `standard:no-trailing-whitespace`
- Added safe fix application for write tasks.
- Added JSON, SARIF, text, HTML, and Markdown reporters.
- Added external provider discovery through `sqldelightCheckRuleSet` and `sqldelightCheckReporter`.
- Added Qodana configuration and Gradle TestKit coverage for the plugin.

### Notes

sqldelight-check is pre-1.0. API names and report schemas may change while the first real-world usage is incorporated.
