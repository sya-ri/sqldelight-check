package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports MySQL character set declarations that use utf8 instead of utf8mb4.
 *
 * MySQL `utf8` is an alias for `utf8mb3`, so this rule points schemas toward
 * the full Unicode `utf8mb4` character set.
 */
public class NoUtf8CharsetRule : RegexRule(
    ruleName = "no-utf8-charset",
    pattern = """\b(?:CHARACTER\s+SET|CHARSET)\s*=?\s*utf8\b(?!mb4)""",
    message = "Use utf8mb4 instead of MySQL utf8.",
    targetCapability = DialectCapability.MySql,
    hashLineComments = true,
)
