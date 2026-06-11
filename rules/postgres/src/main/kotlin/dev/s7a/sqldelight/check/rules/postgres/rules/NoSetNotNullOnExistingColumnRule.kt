package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL ALTER COLUMN SET NOT NULL migrations on existing columns.
 *
 * Existing-column nullability changes should use a separate validation strategy
 * before enforcing the constraint.
 */
public class NoSetNotNullOnExistingColumnRule : RegexRule(
    ruleName = "no-set-not-null-on-existing-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bALTER\s+COLUMN\b(?:(?!;).)*\bSET\s+NOT\s+NULL\b""",
    message = "Avoid SET NOT NULL on existing PostgreSQL columns without a separate validation strategy.",
    targetCapability = DialectCapability.PostgreSql,
)
