# Contribution Guide

## Branches

sqldelight-check uses a stable `main` branch and release-line development branches.

- `main` points at the latest published release.
- `release/0.x` is the development branch for the current pre-1.0 release line.
- Feature, fix, and dependency update pull requests should target `release/0.x`.
- Release preparation is merged into `release/0.x` first, then `main` is advanced to the published release commit after artifacts are published.
- A publish pull request from `release/0.x` to `main` is created automatically when `release/0.x` is updated.
- Do not include agent-specific names in branch names.

## Pull Requests

Open pull requests against `release/0.x` unless a maintainer explicitly asks for another base branch.

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

Publish pull requests target `main` from `release/0.x`. Merge the publish pull request with a merge commit only when the
release branch is ready to become the latest published state.

Do not squash or rebase publish pull requests. A merge commit preserves the release branch commits and keeps the next
publish pull request based on the previous published merge point.

After the publish pull request is merged, publish artifacts from `main`, verify that they are available, then create the
GitHub Release.

`v0.1.1` publishing is not automated yet. Before merging a publish pull request, verify:

- `./gradlew --no-daemon releaseCheck`
- The CI Qodana job is green for the release commit, or run the local Qodana Docker command above with the repository
  `qodana.yaml` configuration when verifying outside CI.
- Gradle plugin metadata.
- Generated reports.
- SQLDelight `2.3.2` core analyzer behavior.
- Built-in rule and reporter discovery.
- External provider discovery through `sqldelightCheckRuleSet` and `sqldelightCheckReporter`.

## Protected Branches

The protected branch set is:

- `main`
- `release/*`

Do not force-push or delete protected branches. Changes should go through pull requests.
Rulesets should allow only merge commits into `main` and only squash merges into `release/*`.
The repository ruleset requires one approving review, except for maintainers listed as pull-request bypass actors.
Required status checks cannot be bypassed.
