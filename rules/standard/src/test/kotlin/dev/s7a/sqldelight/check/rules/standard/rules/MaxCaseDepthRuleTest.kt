package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MaxCaseDepthRuleTest {
    @Test
    fun `reports case expression deeper than configured max depth`() {
        val diagnostics =
            MaxCaseDepthRule().diagnostics(
                """
                selectBucket:
                SELECT CASE
                  WHEN score > 0 THEN CASE WHEN score > 10 THEN 1 ELSE 0 END
                  ELSE 0
                END AS bucket
                FROM player;
                """.asSqlDelightFile(),
                options = mapOf("maxDepth" to "1"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(3, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts case expression at configured max depth`() {
        MaxCaseDepthRule().assertDiagnosticCount(
            """
            selectBucket:
            SELECT CASE WHEN score > 0 THEN 1 ELSE 0 END AS bucket
            FROM player;
            """.asSqlDelightFile(),
            0,
            options = mapOf("maxDepth" to "1"),
        )
    }

    @Test
    fun `rejects invalid max depth option`() {
        assertFailsWith<IllegalArgumentException> {
            MaxCaseDepthRule().diagnostics(cleanPlayerSq, options = mapOf("maxDepth" to "0"))
        }
    }
}
