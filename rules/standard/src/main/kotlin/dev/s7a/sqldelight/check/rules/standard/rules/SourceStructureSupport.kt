package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlSourceBlock
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.api.SqlSourceTokenContext

internal data class SourceSubqueryTableReference(
    val block: SqlSourceBlock,
    val introducer: SqlSourceTokenContext,
    val alias: SqlSourceTokenContext?,
)

internal fun SqlSourceTokenContext.isSourceTerm(term: SqlDialectSourceTerm): Boolean =
    token.normalizedText == term.normalizedText

internal fun SqlSourceStructure.parentBlock(block: SqlSourceBlock): SqlSourceBlock? =
    block.parentBlockIndex?.let(blocks::getOrNull)

internal fun SqlSourceStructure.innermostBlockContaining(
    context: SqlSourceTokenContext,
    kind: SqlSourceBlockKind,
): SqlSourceBlock? {
    var result: SqlSourceBlock? = null
    blocks.forEach { block ->
        val current = result
        if (
            block.kind == kind &&
            block.contains(context) &&
            (current == null || block.size < current.size)
        ) {
            result = block
        }
    }
    return result
}

internal fun SqlSourceStructure.depthOf(
    block: SqlSourceBlock,
    kind: SqlSourceBlockKind,
): Int =
    blocks.count { candidate ->
        candidate.kind == kind && candidate.contains(block)
    }

internal fun SqlSourceStructure.topLevelSubqueryTableReferences(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): List<SourceSubqueryTableReference> =
    blocks
        .asSequence()
        .filter { block -> block.kind == SqlSourceBlockKind.Subquery }
        .mapNotNull { block -> subqueryTableReference(content, sourcePatterns, block) }
        .filter { reference -> reference.introducer.parenthesisDepth == 0 }
        .toList()

private fun SqlSourceStructure.subqueryTableReference(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
    block: SqlSourceBlock,
): SourceSubqueryTableReference? {
    val clause = parentBlock(block)?.takeIf { parent -> parent.kind == SqlSourceBlockKind.Clause } ?: return null
    val beforeSubquery = tokensInBlock(clause).filter { context -> context.index < block.startTokenIndex }
    val introducer =
        beforeSubquery.lastOrNull { context ->
            context.isSourceTerm(SqlDialectSourceTerm.From) || context.isSourceTerm(SqlDialectSourceTerm.Join)
        } ?: return null
    return SourceSubqueryTableReference(
        block = block,
        introducer = introducer,
        alias = aliasAfterSubquery(content, sourcePatterns, clause, block, introducer.parenthesisDepth),
    )
}

private fun SqlSourceStructure.aliasAfterSubquery(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
    clause: SqlSourceBlock,
    block: SqlSourceBlock,
    depth: Int,
): SqlSourceTokenContext? {
    val afterSubquery =
        tokensInBlock(clause)
            .asSequence()
            .filter { context -> context.index >= block.endTokenIndex }
            .filter { context -> context.parenthesisDepth == depth }
            .filter { context -> context.token.text.isIdentifierLike() }
            .toList()
    val first = afterSubquery.aliasTokenOrNull(content, sourcePatterns, block.endOffset, 0, depth) ?: return null
    return if (first.isSourceTerm(SqlDialectSourceTerm.As)) {
        afterSubquery.aliasTokenOrNull(content, sourcePatterns, first.token.endOffset, 1, depth)
    } else {
        first
    }
}

private fun List<SqlSourceTokenContext>.aliasTokenOrNull(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
    previousOffset: Int,
    index: Int,
    depth: Int,
): SqlSourceTokenContext? {
    val token = getOrNull(index) ?: return null
    if (content.hasTableReferenceSeparator(previousOffset, token.token.startOffset, depth)) return null
    if (sourcePatterns.matches(TableReferenceBoundary, normalizedTextsFrom(index))) return null
    return token
}

private fun String.isIdentifierLike(): Boolean =
    firstOrNull()?.let { character -> character == '_' || character.isLetter() } == true

private fun String.hasTableReferenceSeparator(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): Boolean =
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .any { character -> character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth }

private fun List<SqlSourceTokenContext>.normalizedTextsFrom(index: Int): List<String> =
    drop(index).map { context -> context.token.normalizedText }
