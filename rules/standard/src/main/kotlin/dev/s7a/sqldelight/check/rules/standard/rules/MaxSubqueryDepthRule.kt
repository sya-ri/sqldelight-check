package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlSourceBlock
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

private const val DEFAULT_MAX_SUBQUERY_DEPTH = 3

/**
 * Reports SELECT statements nested deeper than the configured subquery block depth.
 */
public class MaxSubqueryDepthRule : Rule {
    private val maxSubqueryDepthOption by positiveIntOption("maxDepth", DEFAULT_MAX_SUBQUERY_DEPTH)

    override val id: RuleId = RuleId("max-subquery-depth")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val maxDepth = context.options[maxSubqueryDepthOption]
        val content = context.file.content
        val structure = context.sourceStructure
        val depths = structure.subqueryDepths()
        structure.blocks
            .filter { block -> block.kind == SqlSourceBlockKind.Subquery }
            .forEach { block ->
                val depth = depths[block] ?: return@forEach
                if (depth <= maxDepth) return@forEach
                val token =
                    structure
                        .tokensInBlock(block)
                        .firstOrNull { context -> context.index > block.startTokenIndex }
                        ?.token
                        ?: return@forEach
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Subquery nesting depth is greater than $maxDepth.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun SqlSourceStructure.subqueryDepths(): Map<SqlSourceBlock, Int> {
    val subqueryBlocks = blocks.filter { it.kind == SqlSourceBlockKind.Subquery }
    if (subqueryBlocks.isEmpty()) return emptyMap()
    data class Event(val tokenIndex: Int, val isClose: Boolean, val block: SqlSourceBlock)
    val events = subqueryBlocks
        .flatMap { block ->
            listOf(
                Event(block.startTokenIndex, false, block),
                Event(block.endTokenIndex, true, block),
            )
        }
        .sortedWith(compareBy({ it.tokenIndex }, { if (it.isClose) 0 else 1 }))
    val depths = HashMap<SqlSourceBlock, Int>(subqueryBlocks.size)
    var depth = 0
    for (event in events) {
        if (event.isClose) {
            depth--
        } else {
            depths[event.block] = depth + 1
            depth++
        }
    }
    return depths
}
