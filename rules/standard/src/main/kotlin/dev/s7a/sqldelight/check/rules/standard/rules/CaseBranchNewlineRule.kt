package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline CASE expressions whose branch keywords do not start their own line.
 */
public class CaseBranchNewlineRule : Rule {
    override val id: RuleId = RuleId("case-branch-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val structure = context.sourceStructure
        structure.blocks
            .filter { block -> block.kind == SqlSourceBlockKind.CaseExpression }
            .forEach { block ->
                if (!content.substring(block.startOffset, block.endOffset).contains('\n')) return@forEach

                val directBranchDepth = structure.tokens[block.startTokenIndex].caseDepth + 1

                structure
                    .tokensInBlock(block)
                    .asSequence()
                    .drop(1)
                    .filter { token ->
                        token.caseDepth == directBranchDepth &&
                            caseBranchTerms.any { term -> token.isSourceTerm(term) }
                    }
                    .forEach { branch ->
                        val line = lines.lineContaining(branch.token.startOffset) ?: return@forEach
                        if (line.firstNonWhitespaceOffset == branch.token.startOffset) return@forEach
                        reporter.report(
                            RuleDiagnostic(
                                severity = defaultSeverity,
                                message = "Multiline CASE branch keywords should start their own line.",
                                file = context.file,
                                range = content.rangeAtOffsets(branch.token.startOffset, branch.token.endOffset),
                                database = context.database,
                                fixes = listOf(content.startOwnLineFix(branch.token.startOffset, "Move CASE branch keyword to its own line")),
                            ),
                        )
                    }
            }
    }
}

private val caseBranchTerms =
    setOf(
        SqlDialectSourceTerm.When,
        SqlDialectSourceTerm.Then,
        SqlDialectSourceTerm.Else,
    )
