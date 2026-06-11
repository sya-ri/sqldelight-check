package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MaxLineLengthRuleTest {
    @Test
    fun `reports long line in sq query raw string`() {
        val diagnostics =
            MaxLineLengthRule().diagnostics(
                """
                selectLongName:
                SELECT id, name, email, phone_number, address_line_1, address_line_2, city, region, postal_code, country, created_at, updated_at FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
        assertEquals(121, diagnostics.single().range?.start?.column)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `reports long migration line in sqm raw string`() {
        val diagnostics =
            MaxLineLengthRule().diagnostics(
                """
                CREATE INDEX player_search_idx ON player(name, email, phone_number, address_line_1, address_line_2, city, region, postal_code, country);
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(1, diagnostics.single().range?.start?.line)
        assertEquals(121, diagnostics.single().range?.start?.column)
    }

    @Test
    fun `reports comments and strings that exceed the limit`() {
        val diagnostics =
            MaxLineLengthRule().diagnostics(
                """
                -- This comment is intentionally long enough to exceed the default limit because SQLFluff LT05 treats source text length uniformly here.
                selectLiteral:
                SELECT 'This string literal is intentionally long enough to exceed the default limit because comments and strings are checked too.';
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(listOf(1, 3), diagnostics.map { diagnostic -> diagnostic.range?.start?.line })
    }

    @Test
    fun `accepts clean sq and sqm files`() {
        MaxLineLengthRule().assertDiagnosticCount(cleanPlayerSq, 0)
        MaxLineLengthRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores blank lines`() {
        val content =
            "\n" +
                "<SP>".repeat(121).withSpaces() +
                "\n" +
                """
                selectAll:
                SELECT id
                FROM player;
                """.asSqlDelightFile()

        MaxLineLengthRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `reports each long line independently`() {
        val content =
            """
            selectLongNames:
            SELECT id, name, email, phone_number, address_line_1, address_line_2, city, region, postal_code, country, created_at, updated_at FROM player;
            selectLongScores:
            SELECT id, score, rank, tier, season, league, wins, losses, draws, goals_for, goals_against, streak, updated_at FROM standings;
            """.asSqlDelightFile()

        MaxLineLengthRule().assertDiagnosticCount(content, 2)
    }

    @Test
    fun `uses configured max length`() {
        val diagnostics =
            MaxLineLengthRule().diagnostics(
                """
                selectShort:
                SELECT id, name FROM player;
                """.asSqlDelightFile(),
                options = mapOf("max" to "20"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(21, diagnostics.single().range?.start?.column)
        assertEquals("Line is longer than 20 characters.", diagnostics.single().message)
    }

    @Test
    fun `can ignore comment lines`() {
        val content =
            """
            -- This comment is intentionally long enough to exceed a small configured line limit.
            selectShort:
            SELECT id, name, score, created_at, updated_at FROM player;
            """.asSqlDelightFile()

        MaxLineLengthRule().assertDiagnosticCount(content, 1, options = mapOf("max" to "40", "ignoreComments" to "true"))
    }

    @Test
    fun `rejects invalid max length option`() {
        assertFailsWith<IllegalArgumentException> {
            MaxLineLengthRule().diagnostics(
                """
                selectShort:
                SELECT id FROM player;
                """.asSqlDelightFile(),
                options = mapOf("max" to "zero"),
            )
        }
    }
}
