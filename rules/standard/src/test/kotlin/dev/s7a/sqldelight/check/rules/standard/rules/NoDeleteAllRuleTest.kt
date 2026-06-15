package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDeleteAllRuleTest {
    @Test
    fun `reports delete with true where predicate`() {
        val diagnostics =
            NoDeleteAllRule().diagnostics(
                """
                deleteAll:
                DELETE FROM player
                WHERE TRUE;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
    }

    @Test
    fun `reports delete with one equals one where predicate`() {
        NoDeleteAllRule().assertDiagnosticCount(
            """
            deleteAll:
            DELETE FROM player
            WHERE 1 = 1;
            """.asSqlDelightFile(),
            expected = 1,
        )
    }

    @Test
    fun `ignores constrained delete`() {
        NoDeleteAllRule().assertDiagnosticCount(
            """
            deleteById:
            DELETE FROM player
            WHERE id = :id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
