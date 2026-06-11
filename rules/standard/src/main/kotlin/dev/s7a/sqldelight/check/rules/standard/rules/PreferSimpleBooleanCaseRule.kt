package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
            if (!token.isKeyword("case")) return@forEachIndexed

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
    if (getOrNull(caseIndex + 1)?.isKeyword("when") != true) return null

    var nestedCaseDepth = 0
    var thenIndex: Int? = null
    var elseIndex: Int? = null
    for (index in caseIndex + 1 until size) {
        val token = get(index)
        when {
            token.isKeyword("case") -> nestedCaseDepth++
            token.isKeyword("end") && nestedCaseDepth == 0 -> {
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
                if (hasTopLevelKeyword(caseIndex + 2, thenTokenIndex, "when")) return null
                if (hasTopLevelKeyword(thenTokenIndex + 2, elseTokenIndex, "when")) return null
                if (hasTopLevelKeyword(elseTokenIndex + 2, index, "when")) return null
                return SimpleBooleanCaseMatch(end = token)
            }
            token.isKeyword("end") -> nestedCaseDepth--
            nestedCaseDepth == 0 && token.isKeyword("then") -> {
                if (thenIndex != null) return null
                thenIndex = index
            }
            nestedCaseDepth == 0 && token.isKeyword("else") -> {
                if (elseIndex != null) return null
                elseIndex = index
            }
        }
    }
    return null
}

private fun List<SqlToken>.hasTopLevelKeyword(
    startIndex: Int,
    endIndex: Int,
    keyword: String,
): Boolean {
    var nestedCaseDepth = 0
    for (index in startIndex until endIndex) {
        val token = get(index)
        when {
            token.isKeyword("case") -> nestedCaseDepth++
            token.isKeyword("end") && nestedCaseDepth > 0 -> nestedCaseDepth--
            nestedCaseDepth == 0 && token.isKeyword(keyword) -> return true
        }
    }
    return false
}

private fun SqlToken.isBooleanLiteral(): Boolean = isKeyword("true") || isKeyword("false")
