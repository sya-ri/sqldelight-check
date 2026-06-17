package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewNameCaseRuleTest {
    @Test
    fun `reports view names that are not snake case`() {
        val content =
            """
            CREATE VIEW ActiveUsers AS
            SELECT id FROM users;
            """.asSqlDelightFile()
        val diagnostics =
            ViewNameCaseRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
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
