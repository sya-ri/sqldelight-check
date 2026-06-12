package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `CASE` expressions that map a predicate directly to boolean literals.
 */
public class PreferSimpleBooleanCaseRule : Rule {
    override val id: RuleId = RuleId("prefer-simple-boolean-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Case)) return@forEachIndexed

            val match = tokens.simpleBooleanCaseMatch(index) ?: return@forEachIndexed
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Prefer a direct boolean predicate instead of CASE returning TRUE or FALSE.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, match.end.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class SimpleBooleanCaseMatch(
    val end: SqlToken,
)

private fun List<SqlToken>.simpleBooleanCaseMatch(caseIndex: Int): SimpleBooleanCaseMatch? {
    if (getOrNull(caseIndex + 1)?.isTerm(SqlDialectSourceTerm.When) != true) return null

    var nestedCaseDepth = 0
    var thenIndex: Int? = null
    var elseIndex: Int? = null
    for (index in caseIndex + 1 until size) {
        val token = get(index)
        when {
            token.isTerm(SqlDialectSourceTerm.Case) -> nestedCaseDepth++
            token.isTerm(SqlDialectSourceTerm.End) && nestedCaseDepth == 0 -> {
                val thenTokenIndex = thenIndex ?: return null
                val elseTokenIndex = elseIndex ?: return null
                val thenValue = getOrNull(thenTokenIndex + 1) ?: return null
                val elseValue = getOrNull(elseTokenIndex + 1) ?: return null
                if (thenTokenIndex + 2 != elseTokenIndex) return null
                if (elseTokenIndex + 2 != index) return null
                if (thenValue.startOffset >= get(elseTokenIndex).startOffset) return null
                if (elseValue.startOffset >= token.startOffset) return null
                if (!thenValue.isBooleanLiteral() || !elseValue.isBooleanLiteral()) return null
                if (thenValue.normalizedText == elseValue.normalizedText) return null
                if (hasTopLevelTerm(caseIndex + 2, thenTokenIndex, SqlDialectSourceTerm.When)) return null
                if (hasTopLevelTerm(thenTokenIndex + 2, elseTokenIndex, SqlDialectSourceTerm.When)) return null
                if (hasTopLevelTerm(elseTokenIndex + 2, index, SqlDialectSourceTerm.When)) return null
                return SimpleBooleanCaseMatch(end = token)
            }
            token.isTerm(SqlDialectSourceTerm.End) -> nestedCaseDepth--
            nestedCaseDepth == 0 && token.isTerm(SqlDialectSourceTerm.Then) -> {
                if (thenIndex != null) return null
                thenIndex = index
            }
            nestedCaseDepth == 0 && token.isTerm(SqlDialectSourceTerm.Else) -> {
                if (elseIndex != null) return null
                elseIndex = index
            }
        }
    }
    return null
}

private fun List<SqlToken>.hasTopLevelTerm(
    startIndex: Int,
    endIndex: Int,
    term: SqlDialectSourceTerm,
): Boolean {
    var nestedCaseDepth = 0
    for (index in startIndex until endIndex) {
        val token = get(index)
        when {
            token.isTerm(SqlDialectSourceTerm.Case) -> nestedCaseDepth++
            token.isTerm(SqlDialectSourceTerm.End) && nestedCaseDepth > 0 -> nestedCaseDepth--
            nestedCaseDepth == 0 && token.isTerm(term) -> return true
        }
    }
    return false
}

private fun SqlToken.isBooleanLiteral(): Boolean = isTerm(SqlDialectSourceTerm.True) || isTerm(SqlDialectSourceTerm.False)
