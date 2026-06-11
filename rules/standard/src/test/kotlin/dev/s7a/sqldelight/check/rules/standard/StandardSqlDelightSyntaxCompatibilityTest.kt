package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.rules.standard.rules.MIGRATION_SQM_PATH
import dev.s7a.sqldelight.check.rules.standard.rules.PLAYER_SQ_PATH
import dev.s7a.sqldelight.check.rules.standard.rules.cleanMigrationSqm
import dev.s7a.sqldelight.check.rules.standard.rules.cleanPlayerSq
import dev.s7a.sqldelight.check.rules.standard.rules.diagnostics
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compatibility tests for SQLDelight-specific `.sq` and `.sqm` syntax shared by the standard rules.
 */
class StandardSqlDelightSyntaxCompatibilityTest {
    @Test
    fun `standard rules accept clean sq files with sqldelight declarations`() {
        StandardRuleSetProvider().ruleProviders().forEach { provider ->
            val rule = provider.create()

            assertEquals(
                emptyList(),
                rule.diagnostics(cleanPlayerSq, path = PLAYER_SQ_PATH),
                "${rule.id} should accept SQLDelight imports, column adapters, and named queries.",
            )
        }
    }

    @Test
    fun `standard rules accept clean sqm migration files`() {
        StandardRuleSetProvider().ruleProviders().forEach { provider ->
            val rule = provider.create()

            assertEquals(
                emptyList(),
                rule.diagnostics(cleanMigrationSqm, path = MIGRATION_SQM_PATH),
                "${rule.id} should accept SQLDelight migration files.",
            )
        }
    }
}
