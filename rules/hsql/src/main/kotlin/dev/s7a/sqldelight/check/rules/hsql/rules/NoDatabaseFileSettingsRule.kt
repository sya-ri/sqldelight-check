package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports HSQL database or file settings in SQLDelight schema and migration sources.
 *
 * These settings change database-wide operational behavior and are safer to keep
 * in database bootstrap or administration code instead of versioned DDL files.
 */
public class NoDatabaseFileSettingsRule : RegexRule(
    ruleName = "no-database-file-settings",
    pattern = """\bSET\s+(?:DATABASE|FILES)\b""",
    message = "Keep HSQL database and file settings out of SQLDelight schema migrations.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.Hsql,
)
