# Examples

This directory contains runnable sqldelight-check example projects.

## Basic

`basic` is a minimal SQLDelight JVM project that applies the local sqldelight-check Gradle plugin through a composite
build.

Run it from the repository root:

```shell
./gradlew -p examples/basic sqldelightCheck
```

The task analyzes `examples/basic/src/main/sqldelight`, then writes JSON, SARIF, and text reports under
`examples/basic/build/reports/sqldelight-check/`.

## Custom Extensions

`custom-extensions` is a SQLDelight JVM project that loads a custom rule set and a custom reporter from included builds:

- `custom-ruleset` contributes `example:no-select-star`.
- `custom-reporter` contributes an `example` text reporter.

Run it from the repository root:

```shell
./gradlew -p examples/custom-extensions sqldelightCheck
```

The custom reporter writes `examples/custom-extensions/build/reports/sqldelight-check/custom.txt`.
