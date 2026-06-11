package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports HSQL TEXT table declarations and source bindings.
 *
 * TEXT tables depend on external delimited files, which makes schema migrations
 * sensitive to runtime file layout and database file-access policy.
 */
public class NoTextTableSourceRule : RegexRule(
    ruleName = "no-text-table-source",
    pattern = """\bCREATE\s+TEXT\s+TABLE\b|\bSET\s+TABLE\b(?:(?!;).)*\bSOURCE\b""",
    message = "Avoid HSQL TEXT table file sources in SQLDelight schema migrations.",
    targetCapability = DialectCapability.Hsql,
)
