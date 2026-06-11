package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports HSQL system operations in SQLDelight schema and migration sources.
 *
 * Operations such as CHECKPOINT, SHUTDOWN, BACKUP, SCRIPT, and bulk import or
 * export are administrative actions rather than schema changes.
 */
public class NoSystemOperationsRule : RegexRule(
    ruleName = "no-system-operations",
    pattern = """\b(?:CHECKPOINT|SHUTDOWN)\b|(?:^|;)\s*SCRIPT\b|\bBACKUP\s+DATABASE\b|\bPERFORM\s+(?:EXPORT|IMPORT)\b""",
    message = "Keep HSQL system operations out of SQLDelight schema migrations.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.Hsql,
)
