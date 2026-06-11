package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.booleanOption
import dev.s7a.sqldelight.check.rule.api.commaSeparatedOption
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports configured words that should not appear in SQL source.
 *
 * Configure blocked words with the comma-separated `words` option.
 */
public class BlockedWordsRule : Rule {
    override val id: String = "blocked-words"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val words = context.options.commaSeparatedOption("words")
        if (words.isEmpty()) return

        val matchCase = context.options.booleanOption("matchCase", defaultValue = false)
        val ignoreComments = context.options.booleanOption("ignoreComments", defaultValue = true)
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
            reporter.reportBlockedWord(context, RuleId(id), defaultSeverity, token.text, token.startOffset, token.endOffset)
        }

        if (!ignoreComments) {
            content.commentRanges().forEach { range ->
                content.reportBlockedWordsInRange(context, reporter, RuleId(id), defaultSeverity, range, blockedWords, matchCase)
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
    ruleId: RuleId,
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
            reporter.reportBlockedWord(context, ruleId, severity, word, start, index)
        }
    }
}

private fun DiagnosticReporter.reportBlockedWord(
    context: RuleContext,
    ruleId: RuleId,
    severity: Severity,
    word: String,
    startOffset: Int,
    endOffset: Int,
) {
    report(
        Diagnostic(
            ruleId = ruleId,
            severity = severity,
            message = "Blocked word '$word' is not allowed.",
            file = context.file,
            range = context.file.content.rangeAtOffsets(startOffset, endOffset),
            database = context.database,
        ),
    )
}

private fun Char.isBlockedWordPart(): Boolean = this == '_' || isLetterOrDigit()
