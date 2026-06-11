package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports PostgreSQL index creation statements that omit CONCURRENTLY.
 */
public class RequireConcurrentIndexRule : RegexPostgresRule(
    ruleName = "require-concurrent-index",
    pattern = """\bCREATE\s+(?:UNIQUE\s+)?INDEX\b(?!\s+CONCURRENTLY\b)""",
    message = "Use CREATE INDEX CONCURRENTLY for PostgreSQL indexes that may be built on live tables.",
)

/**
 * Reports CREATE INDEX CONCURRENTLY inside transaction blocks.
 */
public class NoConcurrentIndexInTransactionRule : RegexPostgresRule(
    ruleName = "no-concurrent-index-in-transaction",
    pattern = """\bBEGIN\b[\s\S]*\bCREATE\s+(?:UNIQUE\s+)?INDEX\s+CONCURRENTLY\b|\bCREATE\s+(?:UNIQUE\s+)?INDEX\s+CONCURRENTLY\b[\s\S]*\bCOMMIT\b""",
    message = "CREATE INDEX CONCURRENTLY cannot run inside a transaction block.",
)

/**
 * Reports PostgreSQL ADD CONSTRAINT statements that omit NOT VALID.
 */
public class RequireNotValidConstraintRule : Rule {
    override val id: RuleId = RuleId("postgres:require-not-valid-constraint")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto
    private val addConstraintRegex = Regex("""\bALTER\s+TABLE\b(?:(?!;).)*\bADD\s+CONSTRAINT\b(?:(?!;).)*;?""", regexOptions)
    private val notValidRegex = Regex("""\bNOT\s+VALID\b""", RegexOption.IGNORE_CASE)

    override fun isApplicable(context: RuleContext): Boolean = context.isPostgreSql()

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText()
        addConstraintRegex.findAll(masked)
            .filterNot { match -> notValidRegex.containsMatchIn(match.value) }
            .forEach { match ->
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Add PostgreSQL constraints as NOT VALID and validate them in a later migration.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

/**
 * Reports PostgreSQL ALTER COLUMN SET NOT NULL migrations on existing columns.
 */
public class NoSetNotNullOnExistingColumnRule : RegexPostgresRule(
    ruleName = "no-set-not-null-on-existing-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bALTER\s+COLUMN\b(?:(?!;).)*\bSET\s+NOT\s+NULL\b""",
    message = "Avoid SET NOT NULL on existing PostgreSQL columns without a separate validation strategy.",
)

/**
 * Reports PostgreSQL ADD COLUMN statements that use volatile defaults.
 */
public class NoAddColumnWithVolatileDefaultRule : RegexPostgresRule(
    ruleName = "no-add-column-with-volatile-default",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bADD\s+COLUMN\b(?:(?!;).)*\bDEFAULT\s+(?:now|current_timestamp|random|gen_random_uuid|uuid_generate_v4)\s*\(""",
    message = "Avoid adding a column with a volatile default in a PostgreSQL migration.",
)

/**
 * Reports PostgreSQL serial pseudo-types when identity columns are preferred.
 */
public class PreferIdentityOverSerialRule : RegexPostgresRule(
    ruleName = "prefer-identity-over-serial",
    pattern = """\b(?:SMALLSERIAL|SERIAL|BIGSERIAL)\b""",
    message = "Prefer GENERATED AS IDENTITY columns over PostgreSQL serial types.",
)

/**
 * Reports PostgreSQL DROP COLUMN migrations.
 */
public class NoDropColumnRule : RegexPostgresRule(
    ruleName = "no-drop-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bDROP\s+COLUMN\b""",
    message = "Avoid dropping PostgreSQL columns in a migration that may run against live application code.",
)

/**
 * Reports PostgreSQL RENAME COLUMN migrations.
 */
public class NoRenameColumnRule : RegexPostgresRule(
    ruleName = "no-rename-column",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bRENAME\s+COLUMN\b""",
    message = "Avoid renaming PostgreSQL columns in a migration that may run against live application code.",
)

/**
 * Reports PostgreSQL RENAME TABLE migrations.
 */
public class NoRenameTableRule : RegexPostgresRule(
    ruleName = "no-rename-table",
    pattern = """\bALTER\s+TABLE\b(?:(?!;).)*\bRENAME\s+TO\b""",
    message = "Avoid renaming PostgreSQL tables in a migration that may run against live application code.",
)

/**
 * Base implementation for PostgreSQL rules that can be evaluated from masked source text.
 */
public abstract class RegexPostgresRule(
    ruleName: String,
    pattern: String,
    private val message: String,
) : Rule {
    override val id: RuleId = RuleId("postgres:$ruleName")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto
    private val regex = Regex(pattern, regexOptions)

    override fun isApplicable(context: RuleContext): Boolean = context.isPostgreSql()

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText()
        regex.findAll(masked).forEach { match ->
            reporter.report(
                Diagnostic(
                    ruleId = id,
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
