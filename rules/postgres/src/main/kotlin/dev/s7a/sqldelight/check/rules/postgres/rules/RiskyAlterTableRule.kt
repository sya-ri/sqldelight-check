package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports PostgreSQL ALTER TABLE statements with operations that commonly take strong locks.
 *
 * The rule focuses on migration patterns that should be planned separately for
 * live PostgreSQL databases.
 */
public class RiskyAlterTableRule : Rule {
    override val id: RuleId = RuleId("postgres:risky-alter-table")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto
    override val targetCapability: DialectCapability = DialectCapabilities.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        content.sqlTokens()
            .toList()
            .sqlStatements()
            .forEach { statement ->
                val alterTable = statement.alterTableStart() ?: return@forEach
                if (!statement.hasRiskyAlterTableOperation()) return@forEach

                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message =
                            "Review this PostgreSQL ALTER TABLE operation because it commonly takes strong locks.",
                        file = context.file,
                        range = content.rangeAtOffsets(alterTable.first.startOffset, alterTable.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.alterTableStart(): Pair<SqlToken, SqlToken>? =
    zipWithNext().firstOrNull { (first, second) ->
        first.isKeyword("alter") && second.isKeyword("table")
    }

private fun List<SqlToken>.hasRiskyAlterTableOperation(): Boolean =
    hasDropColumn() ||
        hasAlterColumnType() ||
        hasAlterColumnSetNotNull() ||
        hasAddConstraintWithoutNotValid()

private fun List<SqlToken>.hasDropColumn(): Boolean =
    zipWithNext().any { (first, second) ->
        first.isKeyword("drop") && second.isKeyword("column")
    }

private fun List<SqlToken>.hasAlterColumnType(): Boolean =
    indices.any { index ->
        getOrNull(index).isKeyword("alter") &&
            getOrNull(index + 1).isKeyword("column") &&
            tokensAfter(index + 2).any { token -> token.isKeyword("type") }
    }

private fun List<SqlToken>.hasAlterColumnSetNotNull(): Boolean =
    indices.any { index ->
        getOrNull(index).isKeyword("alter") &&
            getOrNull(index + 1).isKeyword("column") &&
            tokensAfter(index + 2).windowed(size = 3).any { tokens ->
                tokens[0].isKeyword("set") && tokens[1].isKeyword("not") && tokens[2].isKeyword("null")
            }
    }

private fun List<SqlToken>.hasAddConstraintWithoutNotValid(): Boolean =
    zipWithNext().any { (first, second) ->
        first.isKeyword("add") && second.isKeyword("constraint")
    } && !containsNotValid()

private fun List<SqlToken>.containsNotValid(): Boolean =
    zipWithNext().any { (first, second) ->
        first.isKeyword("not") && second.isKeyword("valid")
    }

private fun List<SqlToken>.tokensAfter(index: Int): List<SqlToken> =
    drop(index).takeWhile { token -> !token.isKeyword("alter") && !token.isKeyword("drop") && !token.isKeyword("add") }

private fun SqlToken?.isKeyword(value: String): Boolean = this?.isKeyword(value) == true
