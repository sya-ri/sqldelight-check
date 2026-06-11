package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL RENAME TABLE migrations.
 *
 * Renaming tables can break live application versions that still reference the
 * old name.
 */
public class NoRenameTableRule : RegexRule(
    ruleName = "no-rename-table",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bRENAME\s+TO\b""",
    message = "Avoid renaming PostgreSQL tables in a migration that may run against live application code.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.PostgreSql,
)
