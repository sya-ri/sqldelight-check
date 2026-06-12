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

Do not push directly to `main`. If `main` needs to advance, merge the publish pull request.

Publishing is not automated yet. Use this checklist for each release.

1. Confirm the release version on the publish branch:

   ```shell
   ./gradlew -q printVersion
   ```

2. Wait for required CI on the release pull request into `release/0.x`.

3. Merge the release pull request into `release/0.x`.
   Use the merge method required by the branch ruleset. Required status checks must be green before merging.

4. Wait for the automatically created publish pull request from `release/0.x` to `main`.

5. Wait for required CI on the publish pull request.

6. Merge the publish pull request into `main` with a merge commit.
   Do not squash or rebase the publish pull request.

7. Publish from the `main` merge commit:

   ```shell
   ./gradlew --no-daemon releaseCheck
   ./gradlew --no-daemon \
       :api:publishAndReleaseToMavenCentral \
       :core:publishAndReleaseToMavenCentral \
       :dialects:dialect-hsql:publishAndReleaseToMavenCentral \
       :dialects:dialect-mysql:publishAndReleaseToMavenCentral \
       :dialects:dialect-postgres:publishAndReleaseToMavenCentral \
       :dialects:dialect-sqlite:publishAndReleaseToMavenCentral \
       :reporter-api:publishAndReleaseToMavenCentral \
       :reporters:html:publishAndReleaseToMavenCentral \
       :reporters:github-annotations:publishAndReleaseToMavenCentral \
       :reporters:json:publishAndReleaseToMavenCentral \
       :reporters:markdown:publishAndReleaseToMavenCentral \
       :reporters:sarif:publishAndReleaseToMavenCentral \
       :reporters:text:publishAndReleaseToMavenCentral \
       :rule-api:publishAndReleaseToMavenCentral \
       :rules:hsql:publishAndReleaseToMavenCentral \
       :rules:mysql:publishAndReleaseToMavenCentral \
       :rules:postgres:publishAndReleaseToMavenCentral \
       :rules:sqlite:publishAndReleaseToMavenCentral \
       :rules:standard:publishAndReleaseToMavenCentral
   ./gradlew --no-daemon :gradle-plugin:publishPlugins
   ```

8. Verify published artifacts:

   ```shell
   curl -fsSL \
       https://plugins.gradle.org/m2/dev/s7a/sqldelight/check/dev.s7a.sqldelight.check.gradle.plugin/<version>/dev.s7a.sqldelight.check.gradle.plugin-<version>.pom
   curl -fsSL \
       https://repo.maven.apache.org/maven2/dev/s7a/sqldelight-check-api/<version>/sqldelight-check-api-<version>.pom
   ```

   Maven Central can lag after a successful publish. The GitHub Release can be created before Central is visible when
   the publish task completed successfully.

9. Create the GitHub Release after publishing.
   Target the `main` merge commit, use tag `v<version>`, and write release notes in this format:

   ````markdown
   ## Highlights

   - sqldelight-check <version> ...

   ## What's Changed

   - ...

   ## Upgrade

   Use version `<version>` for the sqldelight-check Gradle plugin:

   ```kotlin
   plugins {
       id("dev.s7a.sqldelight.check") version "<version>"
   }
   ```

   ## Artifacts

   - `dev.s7a:sqldelight-check-api:<version>`
   - `dev.s7a:sqldelight-check-core:<version>`
   - `dev.s7a:sqldelight-check-dialect-hsql:<version>`
   - `dev.s7a:sqldelight-check-dialect-mysql:<version>`
   - `dev.s7a:sqldelight-check-dialect-postgres:<version>`
   - `dev.s7a:sqldelight-check-dialect-sqlite:<version>`
   - `dev.s7a:sqldelight-check-reporter-api:<version>`
   - `dev.s7a:sqldelight-check-reporter-html:<version>`
   - `dev.s7a:sqldelight-check-reporter-github-annotations:<version>`
   - `dev.s7a:sqldelight-check-reporter-json:<version>`
   - `dev.s7a:sqldelight-check-reporter-markdown:<version>`
   - `dev.s7a:sqldelight-check-reporter-sarif:<version>`
   - `dev.s7a:sqldelight-check-reporter-text:<version>`
   - `dev.s7a:sqldelight-check-rule-api:<version>`
   - `dev.s7a:sqldelight-check-rules-hsql:<version>`
   - `dev.s7a:sqldelight-check-rules-mysql:<version>`
   - `dev.s7a:sqldelight-check-rules-postgres:<version>`
   - `dev.s7a:sqldelight-check-rules-sqlite:<version>`
   - `dev.s7a:sqldelight-check-rules-standard:<version>`
   - Gradle plugin `dev.s7a.sqldelight.check` version `<version>`
   ````

   Do not add `dependencies` snippets for artifacts that the Gradle plugin installs by default. Only include dependency
   snippets when a user must add an optional artifact manually.

Before merging a publish pull request, verify:

- `./gradlew --no-daemon releaseCheck`
- The CI Qodana job is green for the release commit, or run the local Qodana Docker command above with the repository
  `qodana.yaml` configuration when verifying outside CI.
- Gradle plugin metadata.
- Generated reports.
- SQLDelight core analyzer behavior.
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
