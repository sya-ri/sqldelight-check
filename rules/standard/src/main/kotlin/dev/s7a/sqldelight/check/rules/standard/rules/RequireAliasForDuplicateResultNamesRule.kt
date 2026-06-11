package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports repeated visible result names that should be disambiguated with aliases.
 */
public class RequireAliasForDuplicateResultNamesRule : Rule {
    override val id: RuleId = RuleId("require-alias-for-duplicate-result-names")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.sourceSelectClauseTargets().forEach { clause ->
            val seen = mutableSetOf<String>()
            clause.targets.forEach { target ->
                val visibleName = content.visibleResultName(target) ?: return@forEach
                if (seen.add(visibleName.lowercase())) return@forEach
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Duplicate result name '$visibleName' should be disambiguated with an alias.",
                        file = context.file,
                        range = content.rangeAtOffsets(target.startOffset, target.endOffset),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private fun String.visibleResultName(target: SourceSelectTarget): String? {
    val tokens = substring(target.startOffset, target.endOffset).sqlTokens().toList()
    if (tokens.isEmpty()) return null
    if (tokens.size >= 2) {
        val previous = tokens[tokens.lastIndex - 1]
        val last = tokens.last()
        if (previous.isKeyword("as") || previous.endOffset < last.startOffset) return last.text
    }
    return tokens.last().text.takeUnless { name -> name == "*" }
}
