package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CreateConcurrentIndexStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionEndStatement
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TransactionStartStatement
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports CREATE INDEX CONCURRENTLY inside transaction blocks.
 *
 * PostgreSQL rejects concurrent index creation inside an explicit transaction.
 */
public class NoConcurrentIndexInTransactionRule : Rule {
    override val id: RuleId = RuleId("no-concurrent-index-in-transaction")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        var inTransaction = false
        context.file.content
            .sqlTokens()
            .toList()
            .sqlStatements()
            .forEach { statement ->
                val sourcePatterns = context.database.dialect.sourcePatterns
                val startsTransaction = statement.findSourcePattern(TransactionStartStatement, sourcePatterns) != null
                val endsTransaction = statement.findSourcePattern(TransactionEndStatement, sourcePatterns) != null
                val concurrentIndex = statement.findSourcePattern(CreateConcurrentIndexStatementStart, sourcePatterns)

                if (inTransaction && concurrentIndex != null || startsTransaction && concurrentIndex != null) {
                    reporter.reportConcurrentIndex(context, concurrentIndex.startToken, concurrentIndex.endToken)
                }
                inTransaction = (inTransaction || startsTransaction) && !endsTransaction
            }
    }
}

private fun DiagnosticReporter.reportConcurrentIndex(
    context: RuleContext,
    startToken: SqlToken,
    endToken: SqlToken,
) {
    report(
        RuleDiagnostic(
            severity = Severity.Warning,
            message = "CREATE INDEX CONCURRENTLY cannot run inside a transaction block.",
            file = context.file,
            range = context.file.content.rangeAtOffsets(startToken.startOffset, endToken.endOffset),
            database = context.database,
        ),
    )
}
