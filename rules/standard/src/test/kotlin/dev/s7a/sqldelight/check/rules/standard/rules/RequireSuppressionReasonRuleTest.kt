package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireSuppressionReasonRuleTest {
    @Test
    fun `reports disable directives without reasons`() {
        val diagnostics =
            RequireSuppressionReasonRule().diagnostics(
                """
                -- sqldelight-check-disable-next-line standard:no-select-star
                SELECT * FROM player;
                -- sqldelight-check-disable-file
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals("sqldelight-check disable directives should include a reason after '--'.", diagnostics.first().message)
    }

    @Test
    fun `accepts disable directives with reasons`() {
        RequireSuppressionReasonRule().assertDiagnosticCount(
            """
            -- sqldelight-check-disable-next-line standard:no-select-star -- legacy export
            SELECT * FROM player;
            -- sqldelight-check-disable-file -- generated SQLDelight fixture
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores enable directives and ordinary comments`() {
        RequireSuppressionReasonRule().assertDiagnosticCount(
            """
            -- sqldelight-check-enable standard:no-select-star
            -- ordinary comment
            SELECT id FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
