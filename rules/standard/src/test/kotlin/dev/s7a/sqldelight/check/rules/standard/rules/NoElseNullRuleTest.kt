package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoElseNullRuleTest {
    @Test
    fun `reports else null in case expressions`() {
        val content =
            """
            selectPlayerStatus:
            SELECT
              CASE
                WHEN score > 10 THEN 'starter'
                ELSE NULL
              END AS status
            FROM player;
            """.asSqlDelightFile()
        val diagnostics = NoElseNullRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        NoElseNullRule().assertAllFixes(
            content,
            """
            selectPlayerStatus:
            SELECT
              CASE
                WHEN score > 10 THEN 'starter'
              END AS status
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports else null in migration files`() {
        NoElseNullRule().assertDiagnosticCount(
            """
            UPDATE player
            SET nickname = CASE
              WHEN nickname = '' THEN NULL
              ELSE NULL
            END;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts case without else null`() {
        NoElseNullRule().assertDiagnosticCount(
            """
            selectPlayerStatus:
            SELECT
              CASE
                WHEN score > 10 THEN 'starter'
              END AS status,
              CASE
                WHEN score > 0 THEN 'active'
                ELSE 'inactive'
              END AS activity
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports nested case else null branches`() {
        NoElseNullRule().assertDiagnosticCount(
            """
            selectPlayerStatus:
            SELECT CASE
              WHEN score > 10 THEN CASE
                WHEN name IS NULL THEN 'missing'
                ELSE NULL
              END
              ELSE NULL
            END
            FROM player;
            """.asSqlDelightFile(),
            2,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoElseNullRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- CASE WHEN score > 10 THEN name ELSE NULL END
            SELECT 'ELSE NULL', "ELSE NULL", `ELSE NULL`, [ELSE NULL]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
