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
  - `standard:data-type-case`
  - `standard:final-newline`
  - `standard:function-name-case`
  - `standard:keyword-case`
  - `standard:line-ending-lf`
  - `standard:literal-case`
  - `standard:max-blank-lines`
  - `standard:no-consecutive-semicolons`
  - `standard:no-leading-blank-lines`
  - `standard:no-space-after-dot`
  - `standard:no-space-after-opening-parenthesis`
  - `standard:no-space-before-closing-parenthesis`
  - `standard:no-space-before-comma`
  - `standard:no-space-before-dot`
  - `standard:no-space-before-semicolon`
  - `standard:no-tab-indentation`
  - `standard:no-trailing-blank-lines`
  - `standard:no-trailing-whitespace`
  - `standard:space-after-comma`
  - `standard:space-after-line-comment-marker`
  - `standard:space-around-comparison-operators`
- Added safe fix application for write tasks.
- Added JSON, SARIF, text, HTML, and Markdown reporters.
- Added external provider discovery through `sqldelightCheckRuleSet` and `sqldelightCheckReporter`.
- Added Qodana configuration and Gradle TestKit coverage for the plugin.

### Published Artifacts

- Gradle plugin `dev.s7a.sqldelight.check`
- `dev.s7a:sqldelight-check-api:0.1.0`
- `dev.s7a:sqldelight-check-rule-api:0.1.0`
- `dev.s7a:sqldelight-check-reporter-api:0.1.0`
- `dev.s7a:sqldelight-check-adapter-spi:0.1.0`
- `dev.s7a:sqldelight-check-core:0.1.0`
- `dev.s7a:sqldelight-check-rules-standard:0.1.0`
- `dev.s7a:sqldelight-check-reporter-json:0.1.0`
- `dev.s7a:sqldelight-check-reporter-sarif:0.1.0`
- `dev.s7a:sqldelight-check-reporter-text:0.1.0`
- `dev.s7a:sqldelight-check-reporter-html:0.1.0`
- `dev.s7a:sqldelight-check-reporter-markdown:0.1.0`
- `dev.s7a:sqldelight-check-adapter-2-3-2:0.1.0`

### Notes

sqldelight-check is pre-1.0. API names and report schemas may change while the first real-world usage is incorporated.
