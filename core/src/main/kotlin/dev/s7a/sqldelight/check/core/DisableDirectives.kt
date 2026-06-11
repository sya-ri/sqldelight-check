package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Parses sqldelight-check disable comments for a single source file.
 *
 * Directives affect diagnostics produced by configured rules.
 */
internal class DisableDirectives private constructor(
    private val fileRules: RuleMatcher?,
    private val nextLineRules: Map<Int, List<RuleMatcher>>,
    private val disabledLines: List<DisabledLine>,
) {
    fun suppresses(diagnostic: Diagnostic): Boolean {
        val ruleId = diagnostic.ruleId
        if (ruleId in rulesThatMustNotBeSuppressed) return false
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
                    DirectiveCommand.DisableFile -> state.fileRules = directive.matcher
                    DirectiveCommand.DisableNextLine ->
                        state.nextLineRules.getOrPut(lineNumber + 1) {
                            mutableListOf()
                        } += directive.matcher

                    DirectiveCommand.Disable -> activeBlocks += directive.matcher
                    DirectiveCommand.Enable -> activeBlocks.removeMatching(directive.matcher)
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
            val commandText = withoutPrefix.takeWhile { character -> !character.isWhitespace() }
            val command = DirectiveCommand.fromToken(commandText) ?: return null
            val ruleIds = withoutPrefix.drop(command.token.length).withoutReason().trim().ruleIds()
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

        /**
         * Rules that enforce suppression hygiene must remain active even when a
         * directive targets every rule in the file.
         */
        private val rulesThatMustNotBeSuppressed =
            setOf(
                QualifiedRuleId(
                    ruleSetId = RuleSetId("standard"),
                    ruleId = RuleId("require-suppression-reason"),
                ),
            )
    }
}

/**
 * Disable directive command supported in sqldelight-check line comments.
 */
private enum class DirectiveCommand(
    val token: String,
) {
    Disable("disable"),
    Enable("enable"),
    DisableNextLine("disable-next-line"),
    DisableFile("disable-file"),
    ;

    companion object {
        fun fromToken(token: String): DirectiveCommand? =
            entries.firstOrNull { command -> command.token == token }
    }
}

/**
 * Mutable parser state while scanning a source file for disable directives.
 */
private data class ParserState(
    var fileRules: RuleMatcher? = null,
    val nextLineRules: MutableMap<Int, MutableList<RuleMatcher>> = mutableMapOf(),
    val disabledLines: MutableList<DisabledLine> = mutableListOf(),
)

/**
 * Active block-level disable matchers for one source line.
 */
private data class DisabledLine(
    val line: Int,
    val rules: List<RuleMatcher>,
) {
    fun matches(
        diagnosticLine: Int,
        ruleId: QualifiedRuleId,
    ): Boolean = diagnosticLine == line && rules.any { matcher -> matcher.matches(ruleId) }
}

/**
 * Rule ID matcher parsed from a disable directive.
 */
private data class RuleMatcher(
    val ruleIds: Set<String>?,
) {
    fun matches(ruleId: QualifiedRuleId): Boolean = ruleIds == null || ruleId.value in ruleIds
}

/**
 * Parsed disable directive command and target matcher.
 */
private data class Directive(
    val command: DirectiveCommand,
    val matcher: RuleMatcher,
)
