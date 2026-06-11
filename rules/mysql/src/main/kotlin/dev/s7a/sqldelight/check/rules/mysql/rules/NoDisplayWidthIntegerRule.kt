package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports deprecated MySQL integer display width declarations.
 *
 * Display widths no longer affect integer storage and create unnecessary schema
 * noise in modern MySQL versions.
 */
public class NoDisplayWidthIntegerRule : RegexRule(
    ruleName = "no-display-width-integer",
    pattern = """\b(?:TINYINT|SMALLINT|MEDIUMINT|INT|INTEGER|BIGINT)\s*\(\s*\d+\s*\)""",
    message = "Avoid deprecated MySQL integer display widths.",
    targetCapability = DialectCapability.MySql,
    hashLineComments = true,
)
