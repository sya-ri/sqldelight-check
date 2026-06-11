package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL ADD CONSTRAINT statements that omit NOT VALID.
 *
 * Adding constraints as `NOT VALID` lets validation happen in a separate step
 * with reduced migration risk.
 */
public class RequireNotValidConstraintRule : RegexRule(
    ruleName = "require-not-valid-constraint",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bADD\s+CONSTRAINT\b(?:(?!;).)*;?""",
    message = "Add PostgreSQL constraints as NOT VALID and validate them in a later migration.",
    targetCapability = DialectCapability.PostgreSql,
) {
    private val notValidRegex = Regex("""\bNOT\s+VALID\b""", RegexOption.IGNORE_CASE)

    override fun shouldReport(match: MatchResult): Boolean = !notValidRegex.containsMatchIn(match.value)
}
