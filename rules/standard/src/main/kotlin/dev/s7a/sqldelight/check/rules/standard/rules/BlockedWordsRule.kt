package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.booleanOption
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.stringListOption

/**
 * Reports configured words that should not appear in SQL source.
 *
 * Configure blocked words with the comma-separated `words` option.
 */
public class BlockedWordsRule : Rule {
    private val blockedWordsOption by stringListOption("words", emptyList())
    private val matchCaseOption by booleanOption("matchCase", false)
    private val blockedWordsIgnoreCommentsOption by booleanOption("ignoreComments", true)

    override val id: RuleId = RuleId("blocked-words")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val words = context.options[blockedWordsOption]
        if (words.isEmpty()) return

        val matchCase = context.options[matchCaseOption]
        val ignoreComments = context.options[blockedWordsIgnoreCommentsOption]
        val content = context.file.content
        val blockedWords =
            if (matchCase) {
                words.toSet()
            } else {
                words.map { word -> word.lowercase() }.toSet()
            }

        content.sqlTokens().forEach { token ->
            val lookup = if (matchCase) token.text else token.text.lowercase()
            if (lookup !in blockedWords) return@forEach
            reporter.reportBlockedWord(context, defaultSeverity, token.text, token.startOffset, token.endOffset)
        }

        if (!ignoreComments) {
            content.commentRanges().forEach { range ->
                content.reportBlockedWordsInRange(context, reporter, defaultSeverity, range, blockedWords, matchCase)
            }
        }
    }
}

private data class OffsetRange(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.commentRanges(): Sequence<OffsetRange> =
    sequence {
        lineComments().forEach { comment -> yield(OffsetRange(comment.startOffset, comment.endOffset)) }
        blockComments().forEach { comment -> yield(OffsetRange(comment.startOffset, comment.endOffset)) }
    }

private fun String.reportBlockedWordsInRange(
    context: RuleContext,
    reporter: DiagnosticReporter,
    severity: Severity,
    range: OffsetRange,
    blockedWords: Set<String>,
    matchCase: Boolean,
) {
    var index = range.startOffset
    while (index < range.endOffset) {
        if (!this[index].isBlockedWordPart()) {
            index++
            continue
        }

        val start = index
        index++
        while (index < range.endOffset && this[index].isBlockedWordPart()) {
            index++
        }

        val word = substring(start, index)
        val lookup = if (matchCase) word else word.lowercase()
        if (lookup in blockedWords) {
            reporter.reportBlockedWord(context, severity, word, start, index)
        }
    }
}

private fun DiagnosticReporter.reportBlockedWord(
    context: RuleContext,
    severity: Severity,
    word: String,
    startOffset: Int,
    endOffset: Int,
) {
    report(
        RuleDiagnostic(
            severity = severity,
            message = "Blocked word '$word' is not allowed.",
            file = context.file,
            range = context.file.content.rangeAtOffsets(startOffset, endOffset),
            database = context.database,
        ),
    )
}

private fun Char.isBlockedWordPart(): Boolean = this == '_' || isLetterOrDigit()
