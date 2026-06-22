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
 * Reports fully qualified SQLDelight mapped type names that can use imports.
 */
public class PreferImportedMappedTypeRule : Rule {
    override val id: RuleId = RuleId("prefer-imported-mapped-type")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val imports = content.sqlDelightImports()
        val importedNames = imports.mapTo(mutableSetOf()) { import -> import.name }
        val wildcardPackages =
            imports
                .mapNotNull { import -> import.name.removeSuffix(".*").takeIf { import.name.endsWith(".*") } }
                .toSet()
        val importedSimpleNames = importedNames.mapTo(mutableSetOf()) { import -> import.substringAfterLast('.') }
        val mappedTypes =
            content.mappedTypeNames(context.database.dialect.sourcePatterns)
                .filter { type -> type.outerName.contains('.') }
                .groupBy { type -> type.outerName }

        val candidates =
            mappedTypes.mapNotNull { (qualifiedName, occurrences) ->
                val simpleName = qualifiedName.substringAfterLast('.')
                val packageName = qualifiedName.substringBeforeLast('.', missingDelimiterValue = "")
                if (qualifiedName in importedNames) return@mapNotNull null
                if (packageName in wildcardPackages) return@mapNotNull null
                if (simpleName in importedSimpleNames) return@mapNotNull null
                ImportableMappedType(
                    qualifiedName = qualifiedName,
                    simpleName = simpleName,
                    occurrences = occurrences,
                )
            }.filterUniqueSimpleNames()
        if (candidates.isEmpty()) return

        val edits =
            listOf(content.importEdit(imports, candidates.map { candidate -> candidate.qualifiedName })) +
                candidates.flatMap { candidate ->
                    candidate.occurrences.map { occurrence ->
                        TextEdit(
                            range = content.rangeAtOffsets(occurrence.startOffset, occurrence.outerEndOffset),
                            replacement = candidate.simpleName,
                        )
                    }
                }
        val firstOccurrence = candidates.flatMap { candidate -> candidate.occurrences }.minBy { occurrence -> occurrence.startOffset }

        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = "Prefer importing SQLDelight mapped type names.",
                file = context.file,
                range = content.rangeAtOffsets(firstOccurrence.startOffset, firstOccurrence.outerEndOffset),
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Import mapped types",
                            safety = FixSafety.Safe,
                            edits = edits,
                        ),
                    ),
            ),
        )
    }
}

private data class ImportableMappedType(
    val qualifiedName: String,
    val simpleName: String,
    val occurrences: List<MappedTypeName>,
)

private fun List<ImportableMappedType>.filterUniqueSimpleNames(): List<ImportableMappedType> {
    val duplicateSimpleNames =
        groupingBy { candidate -> candidate.simpleName }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    return filterNot { candidate -> candidate.simpleName in duplicateSimpleNames }
}

private fun String.importEdit(
    imports: List<SqlDelightImport>,
    names: List<String>,
): TextEdit {
    val sortedImports = (imports.map { import -> import.name } + names).distinct().sortedWith(importComparator)
    val replacement = sortedImports.joinToString(separator = "\n", postfix = "\n") { import -> "import $import;" }
    if (imports.isEmpty()) {
        return TextEdit(range = rangeAtOffsets(0, 0), replacement = "$replacement\n")
    }

    val start = imports.first().line.startOffset
    val end = imports.last().line.newlineEndOffset
    return TextEdit(range = rangeAtOffsets(start, end), replacement = replacement)
}
