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
