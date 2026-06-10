package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class DataTypeCaseRuleTest {
    @Test
    fun `reports lowercase data types in sq schema`() {
        val diagnostics =
            DataTypeCaseRule().diagnostics(
                """
                CREATE TABLE player (
                  id integer NOT NULL PRIMARY KEY,
                  name text NOT NULL,
                  rating real
                );
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("INTEGER", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports lowercase data types in sqm migration`() {
        DataTypeCaseRule().assertDiagnosticCount(
            """
            ALTER TABLE player ADD COLUMN metadata blob;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts uppercase data types`() {
        DataTypeCaseRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `ignores sqldelight column adapter kotlin types`() {
        DataTypeCaseRule().assertDiagnosticCount(
            """
            import com.example.PlayerId;

            CREATE TABLE player (
              id INTEGER AS PlayerId NOT NULL PRIMARY KEY,
              local_score INTEGER AS Int NOT NULL,
              remote_score INTEGER AS kotlin.Int NOT NULL,
              is_active INTEGER AS kotlin.Boolean NOT NULL
            );

            selectById:
            SELECT id, local_score, remote_score, is_active
            FROM player
            WHERE id = ?;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores data type words used as aliases`() {
        DataTypeCaseRule().assertDiagnosticCount(
            """
            selectScore:
            SELECT score AS int
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        DataTypeCaseRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- integer text blob
            SELECT 'integer', "text", `blob`, [real]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
