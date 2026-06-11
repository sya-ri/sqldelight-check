package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.maskSqlCommentsAndQuotedText
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports SQLite rowid primary keys that do not use the exact INTEGER PRIMARY KEY spelling.
 *
 * SQLite only gives rowid alias behavior to the exact `INTEGER PRIMARY KEY`
 * declaration.
 */
public class PreferIntegerPrimaryKeyRule : RegexSQLiteRule(
    ruleName = "prefer-integer-primary-key",
    pattern = """\b(?:INT|BIGINT|LONG)\s+PRIMARY\s+KEY\b""",
    message = "Use INTEGER PRIMARY KEY for SQLite rowid primary keys.",
)

/**
 * Reports SQLite AUTOINCREMENT usage for schemas that do not need its stricter behavior.
 *
 * AUTOINCREMENT changes rowid reuse semantics and can add overhead compared with
 * the normal SQLite rowid allocator.
 */
public class NoAutoincrementWithoutNeedRule : RegexSQLiteRule(
    ruleName = "no-autoincrement-without-need",
    pattern = """\bAUTOINCREMENT\b""",
    message = "Avoid SQLite AUTOINCREMENT unless its stricter rowid behavior is required.",
)

/**
 * Reports SQLite ALTER TABLE operations that require an explicit table rebuild strategy.
 *
 * The rule flags migration forms that SQLite cannot apply as simple in-place
 * table alterations.
 */
public class NoAlterTableComplexChangeRule : RegexSQLiteRule(
    ruleName = "no-alter-table-complex-change",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\b(?:ALTER\s+COLUMN|ADD\s+CONSTRAINT|DROP\s+CONSTRAINT)\b""",
    message = "Avoid complex SQLite ALTER TABLE changes; rebuild the table explicitly.",
)

/**
 * Reports SQLite composite primary key tables that can consider WITHOUT ROWID.
 *
 * WITHOUT ROWID can reduce duplicate primary-key storage for some composite-key
 * SQLite tables.
 */
public class PreferWithoutRowidForCompositePkRule : Rule {
    override val id: RuleId = RuleId("prefer-without-rowid-for-composite-pk")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapabilities.SQLite

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText()
        Regex("""\bCREATE\s+TABLE\b(?:(?!;).)*\bPRIMARY\s+KEY\s*\(([^)]*,[^)]*)\)(?:(?!;).)*;?""", regexOptions)
            .findAll(masked)
            .filterNot { match -> match.value.contains(Regex("""\bWITHOUT\s+ROWID\b""", RegexOption.IGNORE_CASE)) }
            .forEach { match ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Consider WITHOUT ROWID for SQLite tables with composite primary keys.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

/**
 * Base implementation for SQLite rules that can be evaluated from masked source text.
 *
 * The base class centralizes capability gating and diagnostic range mapping for
 * regex-backed rules.
 */
public abstract class RegexSQLiteRule(
    ruleName: String,
    pattern: String,
    private val message: String,
) : Rule {
    override val id: RuleId = RuleId("$ruleName")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapabilities.SQLite
    private val regex = Regex(pattern, regexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText()
        regex.findAll(masked).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = message,
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}
