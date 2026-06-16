package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferSimpleBooleanCaseRuleTest {
    @Test
    fun `reports case expressions that return predicate truth`() {
        val content =
            """
            selectActive:
            SELECT CASE WHEN score > 10 THEN TRUE ELSE FALSE END AS active
            FROM player;
            """.asSqlDelightFile()
        val diagnostics =
            PreferSimpleBooleanCaseRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports inverse case expressions`() {
        val content =
            """
            updateActive:
            UPDATE player
            SET active = CASE WHEN deleted_at IS NULL THEN FALSE ELSE TRUE END;
            """.asSqlDelightFile()
        val diagnostics = PreferSimpleBooleanCaseRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports boolean case expressions in migration files`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            UPDATE player
            SET active = CASE WHEN score > 0 THEN TRUE ELSE FALSE END;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts case expressions where three valued logic may differ`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            selectActive:
            SELECT
              CASE WHEN score > 10 THEN TRUE END AS nullable_active,
              CASE WHEN score > 10 THEN TRUE ELSE NULL END AS maybe_active,
              CASE WHEN score > 10 THEN 1 ELSE 0 END AS active_int
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts searched case expressions with multiple branches`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            selectActive:
            SELECT CASE
              WHEN score > 10 THEN TRUE
              WHEN score = 0 THEN FALSE
              ELSE FALSE
            END AS active
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts case expressions with non literal branch expressions`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            selectActive:
            SELECT CASE
              WHEN score > 10 THEN TRUE AND verified
              ELSE FALSE
            END AS active
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- CASE WHEN score > 10 THEN TRUE ELSE FALSE END
            SELECT 'CASE WHEN score > 10 THEN TRUE ELSE FALSE END',
              "CASE WHEN score > 10 THEN TRUE ELSE FALSE END",
              `CASE WHEN score > 10 THEN TRUE ELSE FALSE END`,
              [CASE WHEN score > 10 THEN TRUE ELSE FALSE END]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports nested boolean case expressions independently`() {
        PreferSimpleBooleanCaseRule().assertDiagnosticCount(
            """
            selectActive:
            SELECT CASE
              WHEN score > 10 THEN CASE WHEN name IS NULL THEN FALSE ELSE TRUE END
              ELSE FALSE
            END AS active
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }
}
