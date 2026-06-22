package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceAroundComparisonOperatorsRuleTest {
    @Test
    fun `reports unsafe fix in sq query parameter predicate`() {
        val diagnostics =
            SpaceAroundComparisonOperatorsRule().diagnostics(
                """
                selectById:
                SELECT id, name
                FROM player
                WHERE id=?;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        assertEquals(" = ", diagnostics.single().fixes.single().edits.single().replacement)
        SpaceAroundComparisonOperatorsRule().assertAllFixes(
            """
            selectById:
            SELECT id, name
            FROM player
            WHERE id=?;
            """.asSqlDelightFile(),
            """
            selectById:
            SELECT id, name
            FROM player
            WHERE id = ?;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `supports greater than or equal operator in sq named query`() {
        val diagnostics =
            SpaceAroundComparisonOperatorsRule()
                .diagnostics(
                    """
                    selectByMinScore:
                    SELECT id, name
                    FROM player
                    WHERE score>=?;
                    """.asSqlDelightFile(),
                )

        assertEquals(1, diagnostics.size)
        assertEquals(" >= ", diagnostics.single().fixes.single().edits.single().replacement)
        SpaceAroundComparisonOperatorsRule().assertAllFixes(
            """
            selectByMinScore:
            SELECT id, name
            FROM player
            WHERE score>=?;
            """.asSqlDelightFile(),
            """
            selectByMinScore:
            SELECT id, name
            FROM player
            WHERE score >= ?;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `supports not equal and sql not equal operators in sq named query`() {
        val diagnostics =
            SpaceAroundComparisonOperatorsRule()
                .diagnostics(
                    """
                    selectFiltered:
                    SELECT id, name
                    FROM player
                    WHERE score!=? OR name<>'';
                    """.asSqlDelightFile(),
                )

        assertEquals(2, diagnostics.size)
        assertEquals(" != ", diagnostics[0].fixes.single().edits.single().replacement)
        assertEquals(" <> ", diagnostics[1].fixes.single().edits.single().replacement)
        SpaceAroundComparisonOperatorsRule().assertAllFixes(
            """
            selectFiltered:
            SELECT id, name
            FROM player
            WHERE score!=? OR name<>'';
            """.asSqlDelightFile(),
            """
            selectFiltered:
            SELECT id, name
            FROM player
            WHERE score != ? OR name <> '';
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts operators with one space on both sides in clean sq file`() {
        SpaceAroundComparisonOperatorsRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `ignores operators at line boundaries in sq named query`() {
        val content =
            """
            selectById:
            SELECT *
            FROM player
            WHERE id
            = 1;
            """.asSqlDelightFile()

        SpaceAroundComparisonOperatorsRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- id=1
            SELECT 'id=1', "id=1", `id=1`, [id=1]
            FROM player;
            """.asSqlDelightFile()

        SpaceAroundComparisonOperatorsRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `ignores generic SQLDelight mapped types`() {
        val content =
            """
            CREATE TABLE example (
                display_primary_codes TEXT[]
                    AS kotlin.collections.List<com.example.domain.PrimaryCode> NOT NULL DEFAULT '{}',
                display_secondary_codes TEXT[]
                    AS kotlin.collections.Map<kotlin.String, kotlin.collections.List<com.example.domain.SecondaryCode>> NOT NULL
            );
            """.asSqlDelightFile()

        SpaceAroundComparisonOperatorsRule().assertDiagnosticCount(content, 0)
    }
}
