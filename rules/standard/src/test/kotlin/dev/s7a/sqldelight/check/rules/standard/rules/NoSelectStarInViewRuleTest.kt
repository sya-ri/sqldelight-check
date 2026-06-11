package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelectStarInViewRuleTest {
    @Test
    fun `reports select star in view definitions`() {
        val diagnostics =
            NoSelectStarInViewRule().diagnostics(
                """
                CREATE VIEW active_users AS
                SELECT * FROM users WHERE active = 1;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts explicit view columns`() {
        NoSelectStarInViewRule().assertDiagnosticCount(
            """
            CREATE VIEW active_users AS
            SELECT id, name FROM users WHERE active = 1;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
