# Architecture Notes

This document records the implementation boundaries that matter for the
`v0.1.0` API surface.

## SQLDelight Compatibility

sqldelight-check uses SQLDelight's Gradle task model to discover databases,
local `.sq` and `.sqm` source files, and dialect coordinates. The core rule
engine does not invoke the SQLDelight compiler or expose SQLDelight PSI.

The compatibility boundary is intentionally narrow:

- `SqlDelightProjectResolver` reads SQLDelight task properties for database
  name, compilation unit source folders, and dialect configuration.
- `AnalysisInput` contains only stable sqldelight-check data:
  `DatabaseContext` and resolved `SourceFile` values.
- Dialect coordinates are converted to stable `SqlDialect` metadata and passed
  to rules. The dialect implementation is not loaded by core.
- SQLDelight parser and compiler diagnostics remain SQLDelight's own
  responsibility. sqldelight-check reports rule diagnostics only.

This keeps the public rule API independent from SQLDelight and IntelliJ
internals. SQLDelight version compatibility is therefore primarily about
whether the Gradle task model still exposes the database and source folder
shape sqldelight-check reads. TestKit coverage across SQLDelight plugin
versions should catch task model changes.

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
