package dev.s7a.sqldelight.check.api

/**
 * A source block before parent relationships have been resolved.
 */
internal class PendingSqlSourceBlock(
    val kind: SqlSourceBlockKind,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
    val sourcePatternMatch: SqlSourcePatternMatch? = null,
) {
    val size: Int
        get() = endTokenIndex - startTokenIndex

    fun canContain(block: PendingSqlSourceBlock): Boolean {
        if (startTokenIndex > block.startTokenIndex) return false
        if (block.endTokenIndex > endTokenIndex) return false
        val sameRange = startTokenIndex == block.startTokenIndex && endTokenIndex == block.endTokenIndex
        if (!sameRange) return true
        return kind == SqlSourceBlockKind.Statement && block.kind != SqlSourceBlockKind.Statement
    }
}
