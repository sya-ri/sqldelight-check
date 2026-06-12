package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report

internal fun Report.toJsonReport(): JsonReport =
    JsonReport(
        summary =
            JsonSummary(
                diagnostics = diagnostics.size,
                errors = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error },
                warnings = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Warning },
                infos = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Info },
            ),
        diagnostics = diagnostics.map { diagnostic -> diagnostic.toJsonDiagnostic() },
    )

private fun Diagnostic.toJsonDiagnostic(): JsonDiagnostic =
    JsonDiagnostic(
        ruleId = ruleId.value,
        severity = severity.name.lowercase(),
        message = message,
        file = file?.path,
        range = range?.toJsonRange(),
        database = database?.toJsonDatabase(),
        fixes = fixes.map { fix -> fix.toJsonFix() },
    )

private fun DatabaseContext.toJsonDatabase(): JsonDatabase =
    JsonDatabase(
        name = name,
        dialect = dialect.toJsonDialect(),
    )

private fun SqlDialect.toJsonDialect(): JsonDialect =
    JsonDialect(
        ids = ids.map { id -> id.id }.sorted(),
    )

private fun Fix.toJsonFix(): JsonFix =
    JsonFix(
        title = title,
        safety = safety.name.lowercase(),
        edits = edits.map { edit -> edit.toJsonTextEdit() },
    )

private fun TextEdit.toJsonTextEdit(): JsonTextEdit =
    JsonTextEdit(
        range = range.toJsonRange(),
        replacement = replacement,
    )

private fun SourceRange.toJsonRange(): JsonRange =
    JsonRange(
        start = start.toJsonPosition(),
        end = end.toJsonPosition(),
    )

private fun SourcePosition.toJsonPosition(): JsonPosition =
    JsonPosition(line = line, column = column)
