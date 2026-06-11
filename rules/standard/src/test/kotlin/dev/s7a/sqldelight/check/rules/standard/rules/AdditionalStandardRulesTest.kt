package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdditionalStandardRulesTest {
    @Test
    fun `no select star reports wildcard result columns`() {
        NoSelectStarRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT *
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
        NoSelectStarRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `prefer explicit column list reports insert without columns`() {
        PreferExplicitColumnListInInsertRule().assertDiagnosticCount(
            """
            insertPlayer:
            INSERT INTO player
            VALUES (?, ?);
            """.asSqlDelightFile(),
            1,
        )
        PreferExplicitColumnListInInsertRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `delete and update rules require where clauses`() {
        NoDeleteWithoutWhereRule().assertDiagnosticCount(
            """
            deleteAll:
            DELETE FROM player;
            """.asSqlDelightFile(),
            1,
        )
        NoDeleteWithoutWhereRule().assertDiagnosticCount(
            """
            deleteById:
            DELETE FROM player
            WHERE id = ?;
            """.asSqlDelightFile(),
            0,
        )
        NoUpdateWithoutWhereRule().assertDiagnosticCount(
            """
            updateAll:
            UPDATE player
            SET score = 0;
            """.asSqlDelightFile(),
            1,
        )
        NoUpdateWithoutWhereRule().assertDiagnosticCount(
            """
            updateById:
            UPDATE player
            SET score = ?
            WHERE id = ?;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `require result column alias reports computed select targets`() {
        val diagnostics =
            RequireResultColumnAliasRule().diagnostics(
                """
                selectStats:
                SELECT id, count(*), score + 1 AS next_score, max(score) max_score
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `leading wildcard like reports prefix wildcard literals`() {
        NoLeadingWildcardLikeRule().assertDiagnosticCount(
            """
            findPlayers:
            SELECT id
            FROM player
            WHERE name LIKE '%son';
            """.asSqlDelightFile(),
            1,
        )
        NoLeadingWildcardLikeRule().assertDiagnosticCount(
            """
            findPlayers:
            SELECT id
            FROM player
            WHERE name LIKE 'son%';
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `max joins uses configured max`() {
        MaxJoinsRule().assertDiagnosticCount(
            """
            selectGraph:
            SELECT a.id
            FROM a
            JOIN b ON b.a_id = a.id
            JOIN c ON c.b_id = b.id;
            """.asSqlDelightFile(),
            1,
            options = mapOf("max" to "1"),
        )
    }

    @Test
    fun `max subquery depth uses configured maxDepth`() {
        MaxSubqueryDepthRule().assertDiagnosticCount(
            """
            selectNested:
            SELECT id
            FROM player
            WHERE id IN (SELECT player_id FROM score WHERE score IN (SELECT value FROM score_limit));
            """.asSqlDelightFile(),
            1,
            options = mapOf("maxDepth" to "1"),
        )
    }

    @Test
    fun `max case depth uses configured maxDepth`() {
        MaxCaseDepthRule().assertDiagnosticCount(
            """
            selectCase:
            SELECT CASE WHEN score > 0 THEN CASE WHEN score > 10 THEN 1 ELSE 0 END ELSE 0 END AS bucket
            FROM player;
            """.asSqlDelightFile(),
            1,
            options = mapOf("maxDepth" to "1"),
        )
    }

    @Test
    fun `max rules reject invalid options`() {
        assertFailsWith<IllegalArgumentException> {
            MaxJoinsRule().diagnostics(cleanPlayerSq, options = mapOf("max" to "nope"))
        }
        assertFailsWith<IllegalArgumentException> {
            MaxCaseDepthRule().diagnostics(cleanPlayerSq, options = mapOf("maxDepth" to "0"))
        }
    }
}
