package dev.s7a.sqldelight.check.rules.standard.rules

internal data class SharedSqlDelightLabel(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
    val bodyStartOffset: Int,
    val grouped: Boolean,
)

internal fun String.sharedSqlDelightLabels(): Sequence<SharedSqlDelightLabel> =
    sequence {
        linesWithRanges().forEach { line ->
            val first = line.firstNonWhitespaceOffset ?: return@forEach
            if (startsWith("--", first) || startsWith("/*", first)) return@forEach

            var end = first
            if (!getOrNull(end).isSqlDelightLabelStart()) return@forEach
            end++
            while (getOrNull(end).isSqlDelightLabelPart()) {
                end++
            }

            when (getOrNull(end)) {
                ':' -> {
                    if (getOrNull(end + 1) == ':') return@forEach
                    yield(
                        SharedSqlDelightLabel(
                            name = substring(first, end),
                            startOffset = first,
                            endOffset = end,
                            bodyStartOffset = line.newlineEndOffset,
                            grouped = false,
                        ),
                    )
                }
                else -> {
                    val brace = nextSqlCharacterAfter(end)
                    if (brace?.value != '{' || brace.offset > line.endOffset) return@forEach
                    yield(
                        SharedSqlDelightLabel(
                            name = substring(first, end),
                            startOffset = first,
                            endOffset = end,
                            bodyStartOffset = brace.offset + 1,
                            grouped = true,
                        ),
                    )
                }
            }
        }
    }

private fun Char?.isSqlDelightLabelStart(): Boolean = this == '_' || this?.isLetter() == true

private fun Char?.isSqlDelightLabelPart(): Boolean = this == '_' || this?.isLetterOrDigit() == true
