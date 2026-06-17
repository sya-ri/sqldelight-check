package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class FinalNewlineRuleTest {
    @Test
    fun `reports safe fix for sq schema and query file`() {
        val diagnostics = FinalNewlineRule().diagnostics(cleanPlayerSq.removeSuffix("\n"), path = PLAYER_SQ_PATH)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("\n", diagnostics.single().fixes.single().edits.single().replacement)
        FinalNewlineRule().assertAllFixes(cleanPlayerSq.removeSuffix("\n"), cleanPlayerSq, path = PLAYER_SQ_PATH)
    }

    @Test
    fun `accepts sq file that already ends with newline`() {
        FinalNewlineRule().assertDiagnosticCount(cleanPlayerSq, 0, path = PLAYER_SQ_PATH)
    }

    @Test
    fun `accepts sqm migration file that already ends with newline`() {
        FinalNewlineRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts empty file`() {
        FinalNewlineRule().assertDiagnosticCount("", 0)
    }
}
