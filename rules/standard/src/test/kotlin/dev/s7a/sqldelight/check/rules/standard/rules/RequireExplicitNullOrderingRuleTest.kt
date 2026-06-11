package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class RequireExplicitNullOrderingRuleTest {
    @Test
    fun `reports order by directions without null ordering`() {
        val diagnostics =
            RequireExplicitNullOrderingRule().diagnostics(
                """
                selectAll:
                SELECT id, name
                FROM player
                ORDER BY name ASC, score DESC;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.first().severity)
    }

    @Test
    fun `accepts explicit null ordering and no explicit direction`() {
        RequireExplicitNullOrderingRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name ASC NULLS LAST, score DESC NULLS FIRST, id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `does not use null ordering from the next order by item`() {
        RequireExplicitNullOrderingRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name ASC, score DESC NULLS LAST;
            """.asSqlDelightFile(),
            expected = 1,
        )
    }
}
