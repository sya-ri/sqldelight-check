package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.positiveIntOption
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

private const val DEFAULT_MAX_GROUP_STATEMENTS = 3

/**
 * Reports SQLDelight grouped statement blocks that contain too many statements.
 */
public class GroupStatementCountLimitRule : Rule {
    private val maxStatementsOption by positiveIntOption("max", DEFAULT_MAX_GROUP_STATEMENTS)

    override val id: RuleId = RuleId("group-statement-count-limit")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val max = context.options[maxStatementsOption]
        content.groupedStatementBlocks().forEach { block ->
            val count = content.statementCountIn(block)
            if (count <= max) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Grouped SQLDelight statement '${block.name}' contains $count statements; maximum is $max.",
                    file = context.file,
                    range = content.rangeAtOffsets(block.nameStartOffset, block.nameEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class GroupedStatementBlock(
    val name: String,
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val bodyStartOffset: Int,
    val bodyEndOffset: Int,
)

private fun String.groupedStatementBlocks(): Sequence<GroupedStatementBlock> =
    sequence {
        sharedSqlDelightLabels()
            .filter { label -> label.grouped }
            .forEach { label ->
                val open = nextSqlCharacterAfter(label.endOffset) ?: return@forEach
                val close = matchingClosingBraceOffset(open.offset) ?: return@forEach
                yield(
                    GroupedStatementBlock(
                        name = label.name,
                        nameStartOffset = label.startOffset,
                        nameEndOffset = label.endOffset,
                        bodyStartOffset = open.offset + 1,
                        bodyEndOffset = close,
                    ),
                )
            }
    }

private fun String.statementCountIn(block: GroupedStatementBlock): Int =
    sqlCharacters()
        .count { character -> character.offset in block.bodyStartOffset until block.bodyEndOffset && character.value == ';' }
