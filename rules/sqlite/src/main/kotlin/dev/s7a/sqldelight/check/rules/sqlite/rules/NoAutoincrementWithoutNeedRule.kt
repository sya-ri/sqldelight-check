package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports SQLite AUTOINCREMENT usage for schemas that do not need its stricter behavior.
 *
 * AUTOINCREMENT changes rowid reuse semantics and can add overhead compared with
 * the normal SQLite rowid allocator.
 */
public class NoAutoincrementWithoutNeedRule : RegexRule(
    ruleName = "no-autoincrement-without-need",
    pattern = """\bAUTOINCREMENT\b""",
    message = "Avoid SQLite AUTOINCREMENT unless its stricter rowid behavior is required.",
    targetCapability = DialectCapability.SQLite,
)
