package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports SQLite composite primary key tables that can consider WITHOUT ROWID.
 *
 * WITHOUT ROWID can reduce duplicate primary-key storage for some composite-key
 * SQLite tables.
 */
public class PreferWithoutRowidForCompositePkRule : RegexRule(
    ruleName = "prefer-without-rowid-for-composite-pk",
    pattern = """\bCREATE\s+TABLE\b(?:(?!;).)*\bPRIMARY\s+KEY\s*\(([^)]*,[^)]*)\)(?:(?!;).)*;?""",
    message = "Consider WITHOUT ROWID for SQLite tables with composite primary keys.",
    targetCapability = DialectCapability.SQLite,
) {
    private val withoutRowidRegex = Regex("""\bWITHOUT\s+ROWID\b""", RegexOption.IGNORE_CASE)

    override fun shouldReport(match: MatchResult): Boolean = !withoutRowidRegex.containsMatchIn(match.value)
}
