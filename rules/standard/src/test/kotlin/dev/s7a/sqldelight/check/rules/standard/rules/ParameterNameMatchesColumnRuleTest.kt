package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterNameMatchesColumnRuleTest {
    @Test
    fun `reports simple predicate parameters that do not match column names`() {
        val diagnostics =
            ParameterNameMatchesColumnRule().diagnostics(
                """
                findByUser:
                SELECT id
                FROM player
                WHERE user_id = :id;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts parameters matching column names`() {
        ParameterNameMatchesColumnRule().assertDiagnosticCount(
            """
            findByUser:
            SELECT id
            FROM player
            WHERE user_id = :userId;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
