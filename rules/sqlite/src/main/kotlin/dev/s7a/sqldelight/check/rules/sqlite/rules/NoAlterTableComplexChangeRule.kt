package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports SQLite ALTER TABLE operations that require an explicit table rebuild strategy.
 *
 * The rule flags migration forms that SQLite cannot apply as simple in-place
 * table alterations.
 */
public class NoAlterTableComplexChangeRule : RegexRule(
    ruleName = "no-alter-table-complex-change",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\b(?:ALTER\s+COLUMN|ADD\s+CONSTRAINT|DROP\s+CONSTRAINT)\b""",
    message = "Avoid complex SQLite ALTER TABLE changes; rebuild the table explicitly.",
    targetCapability = DialectCapability.SQLite,
)
