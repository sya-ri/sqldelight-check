package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports SQLDelight imports that are not sorted by standard package groups.
 */
public class ImportOrderRule : Rule {
    override val id: RuleId = RuleId("import-order")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val imports = content.sqlDelightImports()
        if (imports.size < 2) return

        val sorted = imports.sortedWith(compareBy<SqlDelightImport> { it.name.importGroupRank() }.thenBy { it.name })
        if (imports.map { it.name } == sorted.map { it.name }) return

        val start = imports.first().line.startOffset
        val end = imports.last().line.newlineEndOffset
        val replacement = sorted.joinToString(separator = "\n", postfix = "\n") { import -> "import ${import.name};" }

        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = "SQLDelight imports should be sorted by package group and name.",
                file = context.file,
                range = content.rangeAtOffsets(start, imports.last().line.endOffset),
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Sort imports",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = content.rangeAtOffsets(start, end), replacement = replacement)),
                        ),
                    ),
            ),
        )
    }
}

private fun String.importGroupRank(): Int =
    when {
        startsWith("kotlin.") -> 0
        startsWith("java.") -> 1
        startsWith("javax.") -> 2
        startsWith("kotlinx.") -> 3
        else -> 4
    }

internal val importComparator: Comparator<String> =
    compareBy<String> { import -> import.importGroupRank() }.thenBy { import -> import }
