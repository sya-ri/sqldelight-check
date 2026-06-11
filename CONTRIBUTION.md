# Contribution Guide

## Branches

sqldelight-check uses `main` while the project is being prepared for its first release. Release branches may be added
after `v0.1.0` is published.

Do not include agent-specific names in branch names.

## Pull Requests

Use short, descriptive titles without agent-specific prefixes. Keep each pull request focused on one behavior, fix, or
release task.

## Quality Checks

Use the Gradle wrapper:

```shell
./gradlew --no-daemon check
```

Run Qodana locally with the repository `qodana.yaml` configuration:

```shell
docker run -it -v "$PWD":/data/project jetbrains/qodana-jvm-community:2026.1
```

The CI workflow also runs Qodana on pull requests and pushes to `main` with `JetBrains/qodana-action` and the
repository `qodana.yaml` configuration. Keep the local Docker image version aligned with `qodana.yaml`.

Before committing Kotlin or Gradle changes, scan for fully qualified type references in implementation and build files
and convert them to imports when practical. Package names, import declarations, plugin IDs, Maven coordinates,
reflection class-name strings, and `ServiceLoader` provider names are intentionally allowed.

When public API changes are intentional, update Kotlin ABI baselines after reviewing the API surface:

```shell
./gradlew --no-daemon updateKotlinAbi
```

## Implementation Notes

- Keep SQL parsing and dialect validation inside SQLDelight's compiler/parser layer. Do not reimplement SQLDelight parsing in rules.
- Public APIs need KDoc.
- If a release-blocking compromise is necessary, leave a `FIXME` in code instead of hiding the issue in transient notes.
- Use `.local/` for working notes and local reference clones. `.local/` is intentionally not committed.

## Publishing

`v0.1.0` publishing is not automated yet. Before a release, verify:

- `./gradlew --no-daemon check`
- `./gradlew --no-daemon dokkaGeneratePublicationHtml dokkaGeneratePublicationJavadoc`
- `./gradlew --no-daemon publishToMavenLocal`
- The CI Qodana job is green for the release commit, or run the local Qodana Docker command above with the repository
  `qodana.yaml` configuration when verifying outside CI.
- Gradle plugin metadata.
- Generated reports.
- SQLDelight `2.3.2` core analyzer behavior.
- Built-in rule and reporter discovery.
- External provider discovery through `sqldelightCheckRuleSet` and `sqldelightCheckReporter`.
