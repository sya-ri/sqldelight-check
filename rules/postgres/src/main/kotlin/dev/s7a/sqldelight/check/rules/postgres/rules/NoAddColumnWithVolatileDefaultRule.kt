package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL ADD COLUMN statements that use volatile defaults.
 *
 * Volatile defaults can rewrite or evaluate many existing rows during the
 * migration.
 */
public class NoAddColumnWithVolatileDefaultRule : RegexRule(
    ruleName = "no-add-column-with-volatile-default",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bADD\s+COLUMN\b(?:(?!;).)*\bDEFAULT\s+(?:now|current_timestamp|random|gen_random_uuid|uuid_generate_v4)\s*\(""",
    message = "Avoid adding a column with a volatile default in a PostgreSQL migration.",
    targetCapability = DialectCapability.PostgreSql,
)
