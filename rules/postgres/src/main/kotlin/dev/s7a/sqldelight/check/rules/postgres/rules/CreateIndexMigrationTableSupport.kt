package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CreateIndexStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CreateTableStatementStart
import dev.s7a.sqldelight.check.dialects.postgres.CreateConcurrentIndexStatementStart
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.SqlTokenMatch
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

internal fun List<SqlToken>.findCreateIndexRequiringConcurrentBuild(context: RuleContext): SqlTokenMatch? {
    if (containsSourcePattern(CreateConcurrentIndexStatementStart, context.database.dialect.sourcePatterns)) {
        return null
    }
    if (targetsTableCreatedEarlierInSameMigration(context)) {
        return null
    }
    return findSourcePattern(CreateIndexStatementStart, context.database.dialect.sourcePatterns)
}

internal fun List<SqlToken>.targetsTableCreatedEarlierInSameMigration(context: RuleContext): Boolean {
    if (context.file.kind != SourceFileKind.Migration) return false

    val targetTableName = createIndexTargetTableName(context.file.content) ?: return false
    val indexStartOffset = firstOrNull()?.startOffset ?: return false
    return context.file.content
        .sqlTokens()
        .toList()
        .sqlStatements()
        .asSequence()
        .takeWhile { statement -> (statement.firstOrNull()?.startOffset ?: Int.MAX_VALUE) < indexStartOffset }
        .mapNotNull { statement -> statement.createTableName(context) }
        .any { createdTableName -> createdTableName == targetTableName }
}

private fun List<SqlToken>.createIndexTargetTableName(content: String): String? {
    val onIndex = indexOfFirst { token -> token.isKeyword("on") }
    if (onIndex == -1) return null

    val tableStartIndex =
        if (getOrNull(onIndex + 1)?.isKeyword("only") == true) {
            onIndex + 2
        } else {
            onIndex + 1
        }
    return relationNameStartingAt(tableStartIndex, content)
}

private fun List<SqlToken>.createTableName(context: RuleContext): String? {
    val createTable =
        findSourcePattern(
            CreateTableStatementStart,
            context.database.dialect.sourcePatterns,
        ) ?: return null
    val tableStartIndex = indexOf(createTable.endToken) + 1
    return relationNameStartingAt(tableStartIndex, context.file.content)
}

private fun List<SqlToken>.relationNameStartingAt(
    startIndex: Int,
    content: String,
): String? {
    val first = getOrNull(startIndex) ?: return null
    val second = getOrNull(startIndex + 1)
    return if (second != null && content.hasSqlDotBetween(first, second)) {
        "${first.normalizedText}.${second.normalizedText}"
    } else {
        first.normalizedText
    }
}

private fun String.hasSqlDotBetween(
    left: SqlToken,
    right: SqlToken,
): Boolean =
    substring(left.endOffset, right.startOffset).any { char -> char == '.' }
