package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupStatementCountLimitRuleTest {
    @Test
    fun `reports grouped statements over configured count`() {
        val diagnostics =
            GroupStatementCountLimitRule().diagnostics(
                """
                updateAll {
                  UPDATE player SET name = :name;
                  UPDATE team SET name = :name;
                }
                """.asSqlDelightFile(),
                options = mapOf("max" to "1"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
    }

    @Test
    fun `accepts grouped statements within default count`() {
        GroupStatementCountLimitRule().assertDiagnosticCount(
            """
            updateAll {
              UPDATE player SET name = :name;
              UPDATE team SET name = :name;
            }
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
