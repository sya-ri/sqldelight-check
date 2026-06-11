# Architecture Notes

This document records the implementation boundaries that matter for the
`v0.1.0` API surface.

## SQLDelight 2.x Compatibility

sqldelight-check uses SQLDelight itself for parser and compiler diagnostics.
The Gradle plugin resolves SQLDelight database metadata and the configured
dialect classpath from the checked project. Core analysis links to the
SQLDelight 2.x compiler API directly and loads the checked project's dialect
implementation through a small child class loader.

The compatibility code in `core.sqldelight` is intentionally narrow:

- `SqlDelight2VersionSupport` accepts stable SQLDelight `2.0.x` through
  `2.3.x` and the explicitly tested `2.4.0-SNAPSHOT`.
- `SqlDelightProjectResolver` reads SQLDelight's Gradle task model through
  SQLDelight `2.3.2` task and property types. It does not use reflective
  method lookup for database name, package name, source folders, or
  compilation-unit discovery.
- `SqlDelight2Analyzer` builds typed SQLDelight compilation-unit and database
  property models, then runs `SqlDelightEnvironment` without reflective
  compiler calls.
- The analyzer is compiled against SQLDelight `2.3.2`. Compatibility with
  earlier stable `2.x` Gradle plugin and dialect combinations is verified by
  Gradle TestKit projects that actually apply those versions.
- The dialect child class loader is only used for `ServiceLoader` discovery of
  the configured `SqlDelightDialect` implementation.
- `SourceFileMatching` maps SQLDelight compiler error paths back to
  sqldelight-check `SourceFile` values.

Those pieces keep the public rule API independent from SQLDelight and IntelliJ
internals while still letting SQLDelight own parsing and dialect validation.
The Gradle plugin treats SQLDelight's Gradle task model as a compatibility
surface for the supported `2.x` line and relies on TestKit coverage across the
declared versions to catch task model changes.

For SQLDelight `3.x`, sqldelight-check should treat compatibility as a larger
update. The current `2.x` analyzer should not grow unbounded fallback logic for
unknown major versions.

## Rule Model And PSI

Built-in and custom rules currently run against sqldelight-check-owned models:

- `RuleContext.file.source` for source text.
- `RuleContext.database` for SQLDelight database and dialect metadata.
- `RuleContext.facts` for stable SQL structure facts extracted by core.
- `RuleContext.options` for resolved rule options.

Rules do not receive SQLDelight PSI or IntelliJ PSI objects. That is deliberate
for `v0.1.0`: PSI types are tied to SQLDelight and IntelliJ implementation
details, while `rule-api` is intended to stay stable for third-party rule set
authors.

If a future rule truly needs PSI-level information, the preferred path is to
extend `SqlFacts` with a stable sqldelight-check model rather than exposing PSI
directly. Exposing PSI should require a separate experimental API because it
would bind rule sets to SQLDelight internals and likely to a narrower version
range.

## Shared Rule Helpers

Several built-in rules use source-text helpers for positions, token scanning,
comment skipping, and statement splitting. These helpers are useful, but they
are not yet part of `rule-api` for two reasons:

- The current helpers are tuned to built-in rule needs and differ slightly by
  dialect rule set.
- Once published from `rule-api`, helper behavior becomes part of the public
  compatibility contract.

For `v0.1.0`, the stable public helper surface remains `SqlFacts` and
`RuleContext`. Source scanners should stay internal until they are consolidated
behind tests that cover SQLDelight `.sq` and `.sqm` syntax, dialect comments,
quoted identifiers, and migration statements.

When a helper graduates to `rule-api`, it should be named around the public
behavior it guarantees, not the current implementation. Good candidates are
source ranges, line indexing, comment/string masking, and conservative token
streams.
