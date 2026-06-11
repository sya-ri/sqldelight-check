@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Parses sqldelight-check disable comments for a single source file.
 *
 * Directives affect diagnostics produced by configured rules.
 */
internal class DisableDirectives private constructor(
    private val fileDirective: DisableDirective?,
    private val nextLineRules: Map<Int, List<DisableDirective>>,
    private val disabledLines: List<DisabledLine>,
    private val disableDirectives: List<DisableDirective>,
) {
    fun suppresses(diagnostic: Diagnostic): Boolean {
        val ruleId = diagnostic.ruleId
        if (ruleId in rulesThatMustNotBeSuppressed) return false
        val line = diagnostic.range?.start?.line ?: return false
        if (fileDirective?.suppresses(ruleId) == true) return true
        if (nextLineRules[line]?.any { directive -> directive.suppresses(ruleId) } == true) return true
        return disabledLines.any { disabledLine -> disabledLine.suppresses(line, ruleId) }
    }

    fun redundantDisableDiagnostics(
        file: SourceFile,
        ruleId: QualifiedRuleId,
        severity: Severity,
        database: DatabaseContext,
    ): List<Diagnostic> =
        disableDirectives
            .filterNot { directive -> directive.used }
            .map { directive ->
                Diagnostic(
                    ruleId = ruleId,
                    severity = severity,
                    message = "sqldelight-check disable directive does not suppress any diagnostics.",
                    file = file,
                    range = directive.range,
                    database = database,
                )
            }

    fun suppressionReasonDiagnostics(
        file: SourceFile,
        ruleId: QualifiedRuleId,
        severity: Severity,
        database: DatabaseContext,
    ): List<Diagnostic> =
        disableDirectives
            .filter { directive -> directive.requiresReason && !directive.hasReason }
            .map { directive ->
                Diagnostic(
                    ruleId = ruleId,
                    severity = severity,
                    message = "sqldelight-check disable directives should include a reason after '--'.",
                    file = file,
                    range = directive.range,
                    database = database,
                )
            }

    internal companion object {
        fun parse(file: SourceFile): DisableDirectives {
            val state = ParserState()
            val activeBlocks = mutableListOf<DisableDirective>()
            file.content.lineSequence().forEachIndexed { index, line ->
                val lineNumber = index + 1
                val directive = line.directive(lineNumber) ?: run {
                    activeBlocks.forEach { activeDirective ->
                        state.disabledLines += DisabledLine(lineNumber, listOf(activeDirective))
                    }
                    return@forEachIndexed
                }
                when (directive.command) {
                    DirectiveCommand.DisableFile -> {
                        val disableDirective = directive.disableDirective()
                        state.fileDirective = disableDirective
                        state.disableDirectives += disableDirective
                    }
                    DirectiveCommand.DisableNextLine ->
                        state.nextLineRules.getOrPut(lineNumber + 1) {
                            mutableListOf()
                        } += directive.disableDirective().also(state.disableDirectives::add)

                    DirectiveCommand.Disable ->
                        activeBlocks += directive.disableDirective().also(state.disableDirectives::add)
                    DirectiveCommand.Enable -> activeBlocks.removeMatching(directive.matcher)
                }
            }
            return DisableDirectives(
                fileDirective = state.fileDirective,
                nextLineRules = state.nextLineRules,
                disabledLines = state.disabledLines,
                disableDirectives = state.disableDirectives,
            )
        }

        private fun MutableList<DisableDirective>.removeMatching(matcher: RuleMatcher) {
            if (matcher.ruleIds == null) {
                clear()
                return
            }
            removeAll { active -> active.matcher.ruleIds == matcher.ruleIds }
        }

        private fun String.directive(lineNumber: Int): Directive? {
            val trimmed = trimStart()
            if (!trimmed.startsWith("--")) return null
            val body = trimmed.removePrefix("--").trimStart()
            if (!body.startsWith("sqldelight-check-")) return null
            val withoutPrefix = body.removePrefix("sqldelight-check-")
            val commandText = withoutPrefix.takeWhile { character -> !character.isWhitespace() }
            val command = DirectiveCommand.fromToken(commandText) ?: return null
            val payload = withoutPrefix.drop(command.token.length)
            val ruleIds = payload.withoutReason().trim().ruleIds()
            return Directive(
                command = command,
                hasReason = payload.hasReason(),
                matcher = RuleMatcher(ruleIds = ruleIds),
                range =
                    SourceRange(
                        start = SourcePosition(line = lineNumber, column = 1),
                        end = SourcePosition(line = lineNumber, column = length + 1),
                    ),
            )
        }

        private fun String.withoutReason(): String {
            val delimiter = indexOf(" --")
            return if (delimiter == -1) this else substring(0, delimiter)
        }

        private fun String.hasReason(): Boolean {
            val delimiter = indexOf(" --")
            return delimiter != -1 && drop(delimiter + 3).isNotBlank()
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
                    ruleSetId = RuleSetId("core"),
                    ruleId = RuleId("require-suppression-reason"),
                ),
                QualifiedRuleId(
                    ruleSetId = RuleSetId("core"),
                    ruleId = RuleId("no-redundant-suppression"),
                ),
            )
    }
}

/**
 * Disable directive command supported in sqldelight-check line comments.
 */
private enum class DirectiveCommand(
    val token: String,
    val requiresReason: Boolean,
) {
    Disable("disable", requiresReason = true),
    Enable("enable", requiresReason = false),
    DisableNextLine("disable-next-line", requiresReason = true),
    DisableFile("disable-file", requiresReason = true),
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
    var fileDirective: DisableDirective? = null,
    val nextLineRules: MutableMap<Int, MutableList<DisableDirective>> = mutableMapOf(),
    val disabledLines: MutableList<DisabledLine> = mutableListOf(),
    val disableDirectives: MutableList<DisableDirective> = mutableListOf(),
)

/**
 * Active block-level disable matchers for one source line.
 */
private data class DisabledLine(
    val line: Int,
    val directives: List<DisableDirective>,
) {
    fun suppresses(
        diagnosticLine: Int,
        ruleId: QualifiedRuleId,
    ): Boolean {
        if (diagnosticLine != line) return false
        return directives.any { directive -> directive.suppresses(ruleId) }
    }
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
 * Disable directive that can be reported when it never suppresses diagnostics.
 */
private data class DisableDirective(
    val matcher: RuleMatcher,
    val requiresReason: Boolean,
    val hasReason: Boolean,
    val range: SourceRange,
) {
    var used: Boolean = false

    fun suppresses(ruleId: QualifiedRuleId): Boolean {
        if (!matcher.matches(ruleId)) return false
        used = true
        return true
    }
}

/**
 * Parsed disable directive command and target matcher.
 */
private data class Directive(
    val command: DirectiveCommand,
    val hasReason: Boolean,
    val matcher: RuleMatcher,
    val range: SourceRange,
) {
    fun disableDirective(): DisableDirective =
        DisableDirective(
            matcher = matcher,
            requiresReason = command.requiresReason,
            hasReason = hasReason,
            range = range,
        )
}
