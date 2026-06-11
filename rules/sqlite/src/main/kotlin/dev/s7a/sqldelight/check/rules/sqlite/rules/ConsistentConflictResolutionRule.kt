package dev.s7a.sqldelight.check.rules.sqlite.rules

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
 * Reports SQLite files that mix multiple conflict-resolution upsert styles.
 *
 * Mixing `OR REPLACE` and `ON CONFLICT` styles makes migration intent harder to
 * audit.
 */
public class ConsistentConflictResolutionRule : Rule {
    override val id: RuleId = RuleId("sqlite:consistent-conflict-resolution")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto
    override val targetCapability: DialectCapability = DialectCapabilities.SQLite

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val conflictStyles =
            content.sqlTokens()
                .toList()
                .sqlStatements()
                .flatMap { statement -> statement.conflictStyles() }
        if (conflictStyles.map { conflictStyle -> conflictStyle.kind }.toSet().size <= 1) return

        conflictStyles.forEach { conflictStyle ->
            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message =
                        "Use one SQLite conflict-resolution style consistently within the same file.",
                    file = context.file,
                    range =
                        content.rangeAtOffsets(
                            conflictStyle.startToken.startOffset,
                            conflictStyle.endToken.endOffset,
                        ),
                    database = context.database,
                ),
            )
        }
    }
}

private data class ConflictStyle(
    val kind: ConflictStyleKind,
    val startToken: SqlToken,
    val endToken: SqlToken,
)

private enum class ConflictStyleKind {
    InsertOrReplace,
    ReplaceInto,
    OnConflictDoUpdate,
}

private fun List<SqlToken>.conflictStyles(): List<ConflictStyle> =
    mapIndexedNotNull { index, token ->
        when {
            token.isKeyword("insert") &&
                getOrNull(index + 1).isKeyword("or") &&
                getOrNull(index + 2).isKeyword("replace") ->
                ConflictStyle(
                    kind = ConflictStyleKind.InsertOrReplace,
                    startToken = token,
                    endToken = get(index + 2),
                )
            token.isKeyword("replace") &&
                getOrNull(index - 1).isKeyword("or").not() &&
                getOrNull(index + 1).isKeyword("into") ->
                ConflictStyle(
                    kind = ConflictStyleKind.ReplaceInto,
                    startToken = token,
                    endToken = get(index + 1),
                )
            token.isKeyword("on") &&
                getOrNull(index + 1).isKeyword("conflict") ->
                onConflictDoUpdate(index, token)
            else -> null
        }
    }

private fun List<SqlToken>.onConflictDoUpdate(
    index: Int,
    token: SqlToken,
): ConflictStyle? {
    val updateToken =
        drop(index + 2)
            .windowed(size = 2)
            .firstOrNull { tokens -> tokens[0].isKeyword("do") && tokens[1].isKeyword("update") }
            ?.get(1)
            ?: return null
    return ConflictStyle(
        kind = ConflictStyleKind.OnConflictDoUpdate,
        startToken = token,
        endToken = updateToken,
    )
}

private fun SqlToken?.isKeyword(value: String): Boolean = this?.isKeyword(value) == true
