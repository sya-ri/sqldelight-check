---
name: fix-existing-bug
description: "Use when fixing an existing reported bug or regression in this repository. Follow a test-first repair workflow: reproduce the reported problem with a failing test, confirm the test fails for the expected reason, update the implementation, and rerun the test to confirm it passes."
---

# Fix Existing Bug

## Workflow

1. Identify the smallest behavior that demonstrates the reported bug.
2. Add or update a focused test that reproduces the problem.
3. Run that test before changing production code and confirm it fails for the reported behavior.
4. Fix the implementation with the smallest appropriate change.
5. Rerun the reproducing test and confirm it passes.
6. Run the relevant broader checks for the touched area when practical.

## Reporting

When summarizing the work, include:

- the failing test that reproduced the bug
- the failure observed before the fix
- the implementation area changed
- the test or check output after the fix
