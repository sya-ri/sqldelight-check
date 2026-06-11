package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.rule.api.maskSqlCommentsAndQuotedText
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports MySQL character set declarations that use utf8 instead of utf8mb4.
 *
 * MySQL `utf8` is an alias for `utf8mb3`, so this rule points schemas toward
 * the full Unicode `utf8mb4` character set.
 */
public class NoUtf8CharsetRule : RegexMySqlRule(
    ruleName = "no-utf8-charset",
    pattern = """\b(?:CHARACTER\s+SET|CHARSET)\s*=?\s*utf8\b(?!mb4)""",
    message = "Use utf8mb4 instead of MySQL utf8.",
)

/**
 * Reports MySQL ALTER TABLE statements that force the COPY algorithm.
 *
 * COPY can rebuild the table and is usually unsafe for online schema changes.
 */
public class NoCopyAlgorithmRule : RegexMySqlRule(
    ruleName = "no-copy-algorithm",
    pattern = """\bALGORITHM\s*=\s*COPY\b""",
    message = "Avoid MySQL ALTER TABLE ALGORITHM=COPY for online migrations.",
)

/**
 * Reports MySQL ALTER TABLE statements that request an exclusive lock.
 *
 * Exclusive locks block concurrent access and are risky for migrations that run
 * against live databases.
 */
public class NoExclusiveLockRule : RegexMySqlRule(
    ruleName = "no-exclusive-lock",
    pattern = """\bLOCK\s*=\s*EXCLUSIVE\b""",
    message = "Avoid MySQL ALTER TABLE LOCK=EXCLUSIVE for online migrations.",
) {
    override val defaultSeverity: Severity = Severity.Error
}

/**
 * Reports MySQL zero date defaults that are rejected by stricter SQL modes.
 *
 * The rule helps avoid schema defaults that fail under `NO_ZERO_DATE` or related
 * production SQL modes.
 */
public class NoZeroDateDefaultRule : Rule {
    override val id: RuleId = RuleId("no-zero-date-default")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql
    private val regex = Regex("""\bDEFAULT\s+['"]0000-00-00(?:\s+\d\d:\d\d:\d\d)?['"]""", regexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        regex.findAll(content).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Avoid zero date defaults in MySQL schemas.",
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}

/**
 * Reports deprecated MySQL integer display width declarations.
 *
 * Display widths no longer affect integer storage and create unnecessary schema
 * noise in modern MySQL versions.
 */
public class NoDisplayWidthIntegerRule : RegexMySqlRule(
    ruleName = "no-display-width-integer",
    pattern = """\b(?:TINYINT|SMALLINT|MEDIUMINT|INT|INTEGER|BIGINT)\s*\(\s*\d+\s*\)""",
    message = "Avoid deprecated MySQL integer display widths.",
)

/**
 * Reports indexes on MySQL TEXT and BLOB columns that omit a prefix length.
 *
 * Prefix lengths are required for many MySQL index definitions over large text
 * and binary column types.
 */
public class RequireIndexPrefixLengthRule : Rule {
    override val id: RuleId = RuleId("require-index-prefix-length")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText(hashLineComments = true)
        val textColumns =
            Regex("""\b([A-Za-z_][A-Za-z0-9_$]*)\s+(?:TINYTEXT|TEXT|MEDIUMTEXT|LONGTEXT|BLOB|MEDIUMBLOB|LONGBLOB)\b""", regexOptions)
                .findAll(masked)
                .map { match -> match.groupValues[1].lowercase() }
                .toSet()
        if (textColumns.isEmpty()) return

        Regex("""\bCREATE\s+(?:UNIQUE\s+)?INDEX\b(?:(?!;).)*\(([^)]*)\)""", regexOptions)
            .findAll(masked)
            .forEach { match ->
                val indexedColumns = match.groupValues[1].split(",").map { column -> column.trim() }
                val missingPrefix =
                    indexedColumns.any { column ->
                        val name = column.takeWhile { character -> character.isLetterOrDigit() || character == '_' || character == '$' }
                        name.lowercase() in textColumns && '(' !in column
                    }
                if (!missingPrefix) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Indexes on MySQL TEXT or BLOB columns should specify a prefix length.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

/**
 * Base implementation for MySQL rules that can be evaluated from masked source text.
 *
 * The base class centralizes capability gating and diagnostic range mapping for
 * regex-backed rules.
 */
public abstract class RegexMySqlRule(
    ruleName: String,
    pattern: String,
    private val message: String,
) : Rule {
    override val id: RuleId = RuleId(ruleName)
    override open val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql
    private val regex = Regex(pattern, regexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText(hashLineComments = true)
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
