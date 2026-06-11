package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ViewNameCaseRuleTest {
    @Test
    fun `reports view names that are not snake case`() {
        val diagnostics =
            ViewNameCaseRule().diagnostics(
                """
                CREATE VIEW ActiveUsers AS
                SELECT id FROM users;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts snake case view names`() {
        ViewNameCaseRule().assertDiagnosticCount(
            """
            CREATE VIEW active_users AS
            SELECT id FROM users;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
