package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.BaselineEntry
import java.io.File
import java.nio.charset.StandardCharsets

internal fun File.readSqldelightCheckBaseline(): Baseline {
    val entries =
        readLines(StandardCharsets.UTF_8)
            .mapIndexedNotNull { index, line -> line.baselineEntryOrNull(index + 1, this) }
            .toSet()
    return Baseline(entries)
}

internal fun File.writeSqldelightCheckBaseline(entries: Collection<BaselineEntry>) {
    parentFile?.mkdirs()
    val content =
        buildString {
            appendLine("# database\truleId\tpath\tline\tcolumn\tmessage")
            entries
                .sortedWith(
                    compareBy<BaselineEntry> { it.database }
                        .thenBy { it.path }
                        .thenBy { it.line }
                        .thenBy { it.column }
                        .thenBy { it.ruleId.value }
                        .thenBy { it.message },
                )
                .forEach { entry ->
                    append(entry.database.escapeBaselineField())
                    append('\t')
                    append(entry.ruleId.value.escapeBaselineField())
                    append('\t')
                    append(entry.path.escapeBaselineField())
                    append('\t')
                    append(entry.line)
                    append('\t')
                    append(entry.column)
                    append('\t')
                    append(entry.message.escapeBaselineField())
                    appendLine()
                }
        }
    writeText(content, StandardCharsets.UTF_8)
}

private fun String.baselineEntryOrNull(
    lineNumber: Int,
    file: File,
): BaselineEntry? {
    if (isBlank() || trimStart().startsWith("#")) return null
    val parts = split('\t')
    require(parts.size == 6) {
        "Invalid sqldelight-check baseline entry at ${file.path}:$lineNumber. Expected 6 tab-separated columns."
    }
    return BaselineEntry(
        database = parts[0].unescapeBaselineField(),
        ruleId = QualifiedRuleId(parts[1].unescapeBaselineField()),
        path = parts[2].unescapeBaselineField(),
        line = parts[3].toIntOrNull() ?: invalidBaselineInt(file, lineNumber, "line", parts[3]),
        column = parts[4].toIntOrNull() ?: invalidBaselineInt(file, lineNumber, "column", parts[4]),
        message = parts[5].unescapeBaselineField(),
    )
}

private fun invalidBaselineInt(
    file: File,
    lineNumber: Int,
    name: String,
    value: String,
): Nothing =
    throw IllegalArgumentException(
        "Invalid sqldelight-check baseline $name at ${file.path}:$lineNumber: $value",
    )

private fun String.unescapeBaselineField(): String {
    val builder = StringBuilder(length)
    var escaping = false
    forEach { character ->
        if (escaping) {
            builder.append(
                when (character) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    '\\' -> '\\'
                    else -> character
                },
            )
            escaping = false
            return@forEach
        }
        if (character == '\\') {
            escaping = true
        } else {
            builder.append(character)
        }
    }
    if (escaping) builder.append('\\')
    return builder.toString()
}

private fun String.escapeBaselineField(): String =
    buildString(length) {
        this@escapeBaselineField.forEach { character ->
            append(
                when (character) {
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    '\\' -> "\\\\"
                    else -> character.toString()
                },
            )
        }
    }
