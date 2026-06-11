package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports SQLite rowid primary keys that do not use the exact INTEGER PRIMARY KEY spelling.
 *
 * SQLite only gives rowid alias behavior to the exact `INTEGER PRIMARY KEY`
 * declaration.
 */
public class PreferIntegerPrimaryKeyRule : RegexRule(
    ruleName = "prefer-integer-primary-key",
    pattern = """\b(?:INT|BIGINT|LONG)\s+PRIMARY\s+KEY\b""",
    message = "Use INTEGER PRIMARY KEY for SQLite rowid primary keys.",
    targetCapability = DialectCapability.SQLite,
)
