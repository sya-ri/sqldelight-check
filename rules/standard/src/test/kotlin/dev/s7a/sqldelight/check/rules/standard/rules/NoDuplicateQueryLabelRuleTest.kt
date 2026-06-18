package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoDuplicateQueryLabelRuleTest {
    @Test
    fun `reports duplicate query labels`() {
        val diagnostics =
            NoDuplicateQueryLabelRule().diagnostics(
                """
                selectAll:
                SELECT id FROM player;

                selectAll:
                SELECT name FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts distinct query and group labels`() {
        NoDuplicateQueryLabelRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT id FROM player;

            updateAll {
              UPDATE player SET name = :name;
            }
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
