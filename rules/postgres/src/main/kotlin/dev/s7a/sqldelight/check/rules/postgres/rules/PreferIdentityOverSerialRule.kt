package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL serial pseudo-types when identity columns are preferred.
 *
 * Identity columns are the modern PostgreSQL mechanism for generated numeric
 * keys.
 */
public class PreferIdentityOverSerialRule : RegexRule(
    ruleName = "prefer-identity-over-serial",
    pattern = """\b(?:SMALLSERIAL|SERIAL|BIGSERIAL)\b""",
    message = "Prefer GENERATED AS IDENTITY columns over PostgreSQL serial types.",
    targetCapability = DialectCapability.PostgreSql,
)
