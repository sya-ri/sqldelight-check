package dev.s7a.sqldelight.check.rules.standard.rules

internal data class SqlDelightImport(
    val name: String,
    val line: LineInfo,
) {
    val nameStartOffset: Int = line.startOffset + line.text.indexOf(name)
    val nameEndOffset: Int = nameStartOffset + name.length
}

internal fun String.sqlDelightImports(): List<SqlDelightImport> =
    linesWithRanges()
        .mapNotNull { line ->
            val trimmed = line.text.trim()
            if (!trimmed.startsWith("import ") || !trimmed.endsWith(";")) return@mapNotNull null
            val name = trimmed.removePrefix("import ").removeSuffix(";").trim()
            if (name.isEmpty()) return@mapNotNull null
            SqlDelightImport(name = name, line = line)
        }
