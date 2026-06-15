package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoUpdateAllRuleTest {
    @Test
    fun `reports update with true where predicate`() {
        val diagnostics =
            NoUpdateAllRule().diagnostics(
                """
                updateAll:
                UPDATE player
                SET score = 0
                WHERE TRUE;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
    }

    @Test
    fun `reports update with one equals one where predicate`() {
        NoUpdateAllRule().assertDiagnosticCount(
            """
            updateAll:
            UPDATE player
            SET score = 0
            WHERE 1 = 1;
            """.asSqlDelightFile(),
            expected = 1,
        )
    }

    @Test
    fun `ignores constrained update`() {
        NoUpdateAllRule().assertDiagnosticCount(
            """
            updateById:
            UPDATE player
            SET score = :score
            WHERE id = :id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
