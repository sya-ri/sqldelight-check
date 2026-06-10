package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceBeforeCommaRuleTest {
    @Test
    fun `reports safe fix in sq named query projection`() {
        val diagnostics =
            NoSpaceBeforeCommaRule().diagnostics(
                """
                selectAll:
                SELECT id , name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports tabs before comma in insert query`() {
        val content =
            """
            insertPlayer:
            INSERT INTO player(id, name, score)
            VALUES (?<TAB>, ?, ?);
            """.asSqlDelightFile().withTabs()

        assertEquals("", NoSpaceBeforeCommaRule().singleReplacement(content))
    }

    @Test
    fun `accepts clean sq and sqm comma placement`() {
        NoSpaceBeforeCommaRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoSpaceBeforeCommaRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- SELECT id , name
            SELECT 'id , name', "id , name", `id , name`, [id , name];
            """.asSqlDelightFile()

        NoSpaceBeforeCommaRule().assertDiagnosticCount(content, 0)
    }
}
