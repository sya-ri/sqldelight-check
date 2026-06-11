# Branch Protection

This repository keeps the intended GitHub branch ruleset in
`.github/rulesets/main.json` so protection settings can be reviewed with code.

The `main` ruleset is intended to:

- require pull requests before updates to the default branch
- require one approving review
- dismiss stale approvals after new pushes
- require review thread resolution
- require the latest push to be approved by someone else
- require the `Build` and `Qodana` checks from the `CI` workflow
- block branch deletion and force pushes

Apply or update the ruleset with the GitHub REST API, for example:

```shell
gh api \
  --method POST \
  /repos/OWNER/REPO/rulesets \
  --input .github/rulesets/main.json
```

If the ruleset already exists, update that ruleset ID instead:

```shell
gh api /repos/OWNER/REPO/rulesets
gh api \
  --method PUT \
  /repos/OWNER/REPO/rulesets/RULESET_ID \
  --input .github/rulesets/main.json
```
