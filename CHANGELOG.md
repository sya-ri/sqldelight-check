# Changelog

## v0.2.1

### Added

- Added `standard:prefer-imported-mapped-type` to prefer SQLDelight imports for fully qualified mapped type names.
- Added shared SQL quoted-range skipping helpers for source text scanning.

### Changed

- Updated Gradle wrapper to 9.5.1.
- Improved PostgreSQL create-index checks to allow non-concurrent indexes for tables created earlier in the same migration.

### Fixed

- Fixed false positives for `standard:constraint-newline` on split column constraints.
- Fixed false positives for `standard:source-indentation` around nested source layout.
- Fixed PostgreSQL statement terminator false positives.
- Fixed source text scanning around comments and quoted SQL identifiers.

## v0.2.0

### Breaking Changes

- Replaced dialect family and capability modeling with `DialectId`.
- Split built-in dialect metadata into separate published modules:
  `sqldelight-check-dialect-hsql`, `sqldelight-check-dialect-mysql`,
  `sqldelight-check-dialect-postgres`, and `sqldelight-check-dialect-sqlite`.
- Removed `RegexRule`; rule implementations now use dialect-aware token and source-pattern matching.

### Added

- Added a dialect-aware source pattern DSL for reusable SQL syntax roles and terms.
- Added source nesting and source block structures for rules that need SQL layout context.
- Added `standard:source-indentation` with configurable indentation size.
- Added built-in dialect metadata for HSQL, MySQL, PostgreSQL, and SQLite as independent provider modules.
- Added PR retargeting workflow support for release branch maintenance.

### Changed

- Migrated standard and dialect-specific rules to use source structure and dialect source patterns where applicable.
- Improved custom dialect authoring docs with the source pattern helpers.
- Clarified release and installation docs for the `release/0.x` flow.

### Fixed

- Fixed clause newline handling for SQLDelight named parameters.
- Fixed self column alias detection for function expressions.
- Fixed Renovate configuration to target the `release/0.x` branch.

## v0.1.1

Maintenance release for the `0.1.x` line.

- Added `sqldelightCheck` to Gradle `check` task execution.
- Marked sqldelight-check Gradle tasks as incompatible with Gradle configuration cache.
- Added `core:no-redundant-suppression` for disable directives that no longer suppress any diagnostics.
- Moved suppression reason enforcement to `core:require-suppression-reason` and documented core diagnostics separately.
- Fixed false positives for `standard:no-delete-without-where` on foreign key `ON DELETE` actions.
- Fixed false positives for `standard:no-update-without-where` on upsert `DO UPDATE` actions.
- Documented suppression reason comments.
- Preserved indentation for multiline `CHECK` constraint closing parentheses.

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
- Added source-text rule analysis for SQLDelight files, including SQLDelight-aware source-shape checks.
  sqldelight-check does not replace SQLDelight parser or compiler diagnostics.
- Added API modules for shared models, rule authoring, reporter authoring, and SQLDelight dialect metadata providers.
- Added built-in standard, PostgreSQL, MySQL, SQLite, and HSQL rule sets. The Gradle plugin installs the official rule
  sets by default and gates dialect-specific rules by detected dialect capabilities.
- Added per-rule and per-rule-set enablement and severity overrides, including database-specific overrides.
- Added inline disable directives with optional reason enforcement.
- Added safe fix application through `sqldelightFix`; unsafe fixes require explicit opt-in.
- Added Gradle diagnostic logging for rule hits at `info`, `warning`, and `error` severity levels.
- Added JSON, SARIF, text, navigable HTML, Markdown, and GitHub Actions annotations reporters.
- Added reporter options in the Gradle DSL, including `prettyPrint` for JSON and SARIF and configurable output files.
- Added external provider discovery through `sqldelightCheckRuleSet`, `sqldelightCheckReporter`, and
  `sqldelightCheckDialects`.
- Added Qodana configuration and Gradle TestKit coverage for the plugin.

### Release Notes

sqldelight-check is pre-1.0. API names and report schemas may change while the first real-world usage is incorporated.
There is no standalone CLI in this release.
