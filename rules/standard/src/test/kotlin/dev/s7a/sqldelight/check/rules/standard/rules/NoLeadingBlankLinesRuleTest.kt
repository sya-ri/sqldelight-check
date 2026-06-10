package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoLeadingBlankLinesRuleTest {
    @Test
    fun `reports safe fix before sq schema file content`() {
        val diagnostics =
            NoLeadingBlankLinesRule().diagnostics(
                """


                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `accepts sq file that starts with import or sql content`() {
        NoLeadingBlankLinesRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoLeadingBlankLinesRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts empty file`() {
        NoLeadingBlankLinesRule().assertDiagnosticCount("", 0)
    }

    @Test
    fun `accepts file with only blank lines`() {
        NoLeadingBlankLinesRule().assertDiagnosticCount("\n\n", 0)
    }
}
