package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferBetweenForInclusiveRangeRuleTest {
    @Test
    fun `reports simple inclusive ranges on the same expression`() {
        val diagnostics =
            PreferBetweenForInclusiveRangeRule().diagnostics(
                """
                selectByScore:
                SELECT id, name
                FROM player
                WHERE score >= :minimumScore AND score <= :maximumScore;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
    }

    @Test
    fun `accepts between and exclusive ranges`() {
        PreferBetweenForInclusiveRangeRule().assertDiagnosticCount(
            """
            selectByScore:
            SELECT id, name
            FROM player
            WHERE score BETWEEN :minimumScore AND :maximumScore
               OR score > :low AND score < :high;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
