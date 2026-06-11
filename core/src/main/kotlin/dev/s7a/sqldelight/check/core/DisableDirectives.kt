package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Parses sqldelight-check disable comments for a single source file.
 *
 * Directives intentionally affect rule diagnostics only. SQLDelight compiler
 * diagnostics are left visible so invalid SQL cannot be hidden by lint
 * configuration comments.
 */
internal class DisableDirectives private constructor(
    private val fileRules: RuleMatcher?,
    private val nextLineRules: Map<Int, List<RuleMatcher>>,
    private val disabledLines: List<DisabledLine>,
) {
    fun suppresses(diagnostic: Diagnostic): Boolean {
        val ruleId = diagnostic.ruleId ?: return false
        if (ruleId.value in unsuppressibleRuleIds) return false
        val line = diagnostic.range?.start?.line ?: return false
        if (fileRules?.matches(ruleId) == true) return true
        if (nextLineRules[line]?.any { matcher -> matcher.matches(ruleId) } == true) return true
        return disabledLines.any { disabledLine -> disabledLine.matches(line, ruleId) }
    }

    internal companion object {
        fun parse(file: SourceFile): DisableDirectives {
            val state = ParserState()
            val activeBlocks = mutableListOf<RuleMatcher>()
            file.content.lineSequence().forEachIndexed { index, line ->
                val lineNumber = index + 1
                val directive = line.directive() ?: run {
                    activeBlocks.forEach { matcher ->
                        state.disabledLines += DisabledLine(lineNumber, listOf(matcher))
                    }
                    return@forEachIndexed
                }

                when (directive.command) {
                    "disable-file" -> state.fileRules = directive.matcher
                    "disable-next-line" ->
                        state.nextLineRules.getOrPut(lineNumber + 1) {
                            mutableListOf()
                        } += directive.matcher

                    "disable" -> activeBlocks += directive.matcher
                    "enable" -> activeBlocks.removeMatching(directive.matcher)
                }
            }
            return DisableDirectives(
                fileRules = state.fileRules,
                nextLineRules = state.nextLineRules,
                disabledLines = state.disabledLines,
            )
        }

        private fun MutableList<RuleMatcher>.removeMatching(matcher: RuleMatcher) {
            if (matcher.ruleIds == null) {
                clear()
                return
            }
            removeAll { active -> active.ruleIds == matcher.ruleIds }
        }

        private fun String.directive(): Directive? {
            val trimmed = trimStart()
            if (!trimmed.startsWith("--")) return null
            val body = trimmed.removePrefix("--").trimStart()
            if (!body.startsWith("sqldelight-check-")) return null
            val withoutPrefix = body.removePrefix("sqldelight-check-")
            val command = withoutPrefix.takeWhile { character -> !character.isWhitespace() }
            if (command !in directiveCommands) return null
            val ruleIds = withoutPrefix.drop(command.length).withoutReason().trim().ruleIds()
            return Directive(command = command, matcher = RuleMatcher(ruleIds = ruleIds))
        }

        private fun String.withoutReason(): String {
            val delimiter = indexOf(" --")
            return if (delimiter == -1) this else substring(0, delimiter)
        }

        private fun String.ruleIds(): Set<String>? {
            if (isBlank()) return null
            val ids =
                split(Regex("""[\s,]+"""))
                    .map { token -> token.trim() }
                    .filter { token -> token.isNotEmpty() }
                    .toSet()
            return ids.ifEmpty { null }
        }

        private val directiveCommands = setOf("disable", "enable", "disable-next-line", "disable-file")

        private val unsuppressibleRuleIds = setOf("standard:require-suppression-reason")
    }
}

private data class ParserState(
    var fileRules: RuleMatcher? = null,
    val nextLineRules: MutableMap<Int, MutableList<RuleMatcher>> = mutableMapOf(),
    val disabledLines: MutableList<DisabledLine> = mutableListOf(),
)

private data class DisabledLine(
    val line: Int,
    val rules: List<RuleMatcher>,
) {
    fun matches(
        diagnosticLine: Int,
        ruleId: RuleId,
    ): Boolean = diagnosticLine == line && rules.any { matcher -> matcher.matches(ruleId) }
}

private data class RuleMatcher(
    val ruleIds: Set<String>?,
) {
    fun matches(ruleId: RuleId): Boolean = ruleIds == null || ruleId.value in ruleIds
}

private data class Directive(
    val command: String,
    val matcher: RuleMatcher,
)
