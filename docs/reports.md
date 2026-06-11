# Report Outputs

sqldelight-check writes reports under `build/reports/sqldelight-check/` by default.

Built-in reporters:

| Reporter | Default | Output |
| --- | --- | --- |
| `json` | Enabled | Machine-readable summary and diagnostics. |
| `sarif` | Enabled | SARIF 2.1.0 for code scanning and artifact upload. |
| `text` | Enabled | Compact human-readable diagnostic count. |
| `html` | Disabled | Navigable diagnostics table for uploaded CI artifacts. |
| `markdown` | Disabled | Summary and diagnostics table for GitHub Actions job summaries. |
| `github-annotations` | Auto on GitHub Actions | Workflow command annotations for changed files and check logs. |

Enable optional reports in `build.gradle.kts`:

```kotlin
sqldelightCheck {
    reports {
        html {
            required.set(true)
        }
        markdown {
            required.set(true)
        }
        githubAnnotations {
            required.set(false)
        }
    }
}
```

Reporter-specific options can be set with `options`; the built-in JSON and SARIF reporters also expose typed
`prettyPrint` configuration.

```kotlin
sqldelightCheck {
    reports {
        json {
            prettyPrint.set(true)
        }
        sarif {
            prettyPrint.set(true)
        }
    }
}
```

## Visual Preview

The HTML report is the recommended artifact when reviewers need to inspect diagnostics visually.

## GitHub Actions

To publish annotations in GitHub Actions, run `sqldelightCheck` and print the generated workflow command file after the
task, including on failure:

```yaml
- name: Run sqldelight-check
  id: sqldelight-check
  run: ./gradlew sqldelightCheck

- name: Publish sqldelight-check annotations
  if: always()
  run: |
    if [ -f build/reports/sqldelight-check/report.github-annotations ]; then
      cat build/reports/sqldelight-check/report.github-annotations
    fi
```

Upload the HTML, Markdown, JSON, and SARIF files as build artifacts when they are useful to reviewers:

```yaml
- name: Upload sqldelight-check reports
  if: always()
  uses: actions/upload-artifact@v6
  with:
    name: sqldelight-check-reports
    path: build/reports/sqldelight-check/
```

SARIF and GitHub annotation paths are written relative to `GITHUB_WORKSPACE` on GitHub Actions so uploaded reports match
the checkout root. Outside GitHub Actions, paths are relative to the Gradle root project. If the Gradle root is nested
under the repository checkout, pass an explicit report root.

When the Gradle wrapper is at the checkout root:

```shell
./gradlew -p path/to/gradle-root -PsqldelightCheck.reportRoot="$PWD" sqldelightCheck
```

When the Gradle wrapper is inside the nested Gradle root:

```shell
repo_root="$PWD"
cd path/to/gradle-root
./gradlew -PsqldelightCheck.reportRoot="$repo_root" sqldelightCheck
```
