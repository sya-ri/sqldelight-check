package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLite migration files that disable foreign keys without restoring them.
 */
public class ForeignKeysRestoredRule : Rule {
    override val id: RuleId = RuleId("sqlite:foreign-keys-restored")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun isApplicable(context: RuleContext): Boolean =
        DialectCapabilities.SQLite in context.database.dialect.capabilities &&
            context.file.path.endsWith(".sqm")

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
            Diagnostic(
                ruleId = id,
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
