package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports MySQL ALTER TABLE statements that request an exclusive lock.
 *
 * Exclusive locks block concurrent access and are risky for migrations that run
 * against live databases.
 */
public class NoExclusiveLockRule : RegexRule(
    ruleName = "no-exclusive-lock",
    pattern = """\bLOCK\s*=\s*EXCLUSIVE\b""",
    message = "Avoid MySQL ALTER TABLE LOCK=EXCLUSIVE for online migrations.",
    defaultSeverity = Severity.Error,
    targetCapability = DialectCapability.MySql,
    hashLineComments = true,
)
