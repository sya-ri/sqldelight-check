package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceAroundBinaryOperatorsRuleTest {
    @Test
    fun `reports unsafe fix for arithmetic operators`() {
        val diagnostics =
            SpaceAroundBinaryOperatorsRule().diagnostics(
                """
                selectAdjustedScore:
                SELECT score+1, score-1, score*2, score/2, score%2
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(5, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals(" + ", diagnostics.first().fixes.single().edits.single().replacement)
        SpaceAroundBinaryOperatorsRule().assertAllFixes(
            """
            selectAdjustedScore:
            SELECT score+1, score-1, score*2, score/2, score%2
            FROM player;
            """.asSqlDelightFile(),
            """
            selectAdjustedScore:
            SELECT score + 1, score - 1, score * 2, score / 2, score % 2
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports unsafe fix for concatenation operator`() {
        val diagnostics =
            SpaceAroundBinaryOperatorsRule().diagnostics(
                """
                selectDisplayName:
                SELECT first_name||' '||last_name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(" || ", diagnostics.first().fixes.single().edits.single().replacement)
        SpaceAroundBinaryOperatorsRule().assertAllFixes(
            """
            selectDisplayName:
            SELECT first_name||' '||last_name
            FROM player;
            """.asSqlDelightFile(),
            """
            selectDisplayName:
            SELECT first_name || ' ' || last_name
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts spaced binary operators and select star`() {
        SpaceAroundBinaryOperatorsRule().assertDiagnosticCount(
            """
            selectAdjustedScore:
            SELECT *, score + 1, score - 1, score * 2, score / 2, first_name || last_name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores unary signs and count star`() {
        SpaceAroundBinaryOperatorsRule().assertDiagnosticCount(
            """
            selectConstants:
            SELECT +2, -4, COUNT(*)
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports operators in sqldelight statements`() {
        val diagnostics =
            SpaceAroundBinaryOperatorsRule().diagnostics(
                """
                updateScore:
                UPDATE player
                SET score=score+1
                WHERE id = ?;

                selectScoreBucket:
                SELECT score/10, score%10
                FROM player
                WHERE score>=0;
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
    }

    @Test
    fun `reports operators in migration files`() {
        val diagnostics =
            SpaceAroundBinaryOperatorsRule().diagnostics(
                """
                UPDATE player
                SET score=score+10
                WHERE score<0;

                INSERT INTO player_score_summary(total_score)
                SELECT SUM(score)+COUNT(*)
                FROM player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(2, diagnostics.size)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        SpaceAroundBinaryOperatorsRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- score+1
            SELECT 'score+1', "score+1", `score+1`, [score+1]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
