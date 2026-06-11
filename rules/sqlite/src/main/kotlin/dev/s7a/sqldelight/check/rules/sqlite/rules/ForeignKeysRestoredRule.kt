package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLite migration files that disable foreign keys without restoring them.
 *
 * The rule checks migration text for `PRAGMA foreign_keys = OFF` without a
 * matching restore to `ON`.
 */
public class ForeignKeysRestoredRule : Rule {
    override val id: RuleId = RuleId("foreign-keys-restored")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.SQLite

    override fun isApplicable(context: RuleContext): Boolean = context.file.kind == SourceFileKind.Migration

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val pragmas = content.sqlTokens().toList().foreignKeyPragmas()
        val disabledPragma = pragmas.firstOrNull { pragma -> pragma.value == ForeignKeyPragmaValue.Off } ?: return
        val restoredLater =
            pragmas.any { pragma ->
                pragma.value == ForeignKeyPragmaValue.On && pragma.token.startOffset > disabledPragma.token.startOffset
            }
        if (restoredLater) return

        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message =
                    "Restore SQLite foreign key enforcement with PRAGMA foreign_keys = ON later in the migration.",
                file = context.file,
                range = content.rangeAtOffsets(disabledPragma.token.startOffset, disabledPragma.token.endOffset),
                database = context.database,
            ),
        )
    }
}

private data class ForeignKeyPragma(
    val value: ForeignKeyPragmaValue,
    val token: SqlToken,
)

private enum class ForeignKeyPragmaValue {
    Off,
    On,
}

private fun List<SqlToken>.foreignKeyPragmas(): List<ForeignKeyPragma> =
    mapIndexedNotNull { index, token ->
        if (!token.isKeyword("pragma")) return@mapIndexedNotNull null
        val nameToken = getOrNull(index + 1)
        if (nameToken?.isKeyword("foreign_keys") != true) return@mapIndexedNotNull null

        val valueToken = statementTokensAfter(index).firstOrNull { candidate ->
            candidate.isKeyword("off") || candidate.isKeyword("on")
        } ?: return@mapIndexedNotNull null

        ForeignKeyPragma(
            value = if (valueToken.isKeyword("off")) ForeignKeyPragmaValue.Off else ForeignKeyPragmaValue.On,
            token = token,
        )
    }

private fun List<SqlToken>.statementTokensAfter(startIndex: Int): List<SqlToken> {
    val result = mutableListOf<SqlToken>()
    var index = startIndex + 1
    while (index < size && this[index].text != ";") {
        result += this[index]
        index++
    }
    return result
}
