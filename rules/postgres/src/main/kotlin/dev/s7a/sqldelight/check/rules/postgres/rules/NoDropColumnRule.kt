package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL DROP COLUMN migrations.
 *
 * Dropping columns can break live application versions that still read or write
 * the old schema.
 */
public class NoDropColumnRule : RegexRule(
    ruleName = "no-drop-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bDROP\s+COLUMN\b""",
    message = "Avoid dropping PostgreSQL columns in a migration that may run against live application code.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.PostgreSql,
)
