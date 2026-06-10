package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceAfterLineCommentMarkerRuleTest {
    @Test
    fun `reports safe fix for sq line comment marker`() {
        val diagnostics =
            SpaceAfterLineCommentMarkerRule().diagnostics(
                """
                --Player lookup queries.
                selectAll:
                SELECT id, name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `accepts spaced and empty line comments`() {
        SpaceAfterLineCommentMarkerRule().assertDiagnosticCount(
            """
            -- Player lookup queries.
            --
            selectAll:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports migration line comments`() {
        SpaceAfterLineCommentMarkerRule().assertDiagnosticCount(
            """
            --Add player score.
            ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `ignores line comment marker inside strings and quoted identifiers`() {
        SpaceAfterLineCommentMarkerRule().assertDiagnosticCount(
            """
            selectLiteral:
            SELECT '--not a comment', "--not a comment", `--not a comment`, [--not a comment]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
