package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL RENAME COLUMN migrations.
 *
 * Renaming columns can break live application versions that still reference the
 * old name.
 */
public class NoRenameColumnRule : RegexRule(
    ruleName = "no-rename-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bRENAME\s+COLUMN\b""",
    message = "Avoid renaming PostgreSQL columns in a migration that may run against live application code.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.PostgreSql,
)
