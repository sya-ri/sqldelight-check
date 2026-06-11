package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockedWordsRuleTest {
    @Test
    fun `reports configured words in sql tokens`() {
        val diagnostics =
            BlockedWordsRule().diagnostics(
                """
                selectDeprecated:
                SELECT deprecated
                FROM player;
                """.asSqlDelightFile(),
                options = mapOf("words" to "deprecated,legacy"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Blocked word 'deprecated' is not allowed.", diagnostics.single().message)
    }

    @Test
    fun `matches blocked words case insensitively by default`() {
        BlockedWordsRule().assertDiagnosticCount(
            """
            selectDeprecated:
            SELECT Deprecated
            FROM player;
            """.asSqlDelightFile(),
            1,
            options = mapOf("words" to "deprecated"),
        )
    }

    @Test
    fun `honors case sensitive matching`() {
        BlockedWordsRule().assertDiagnosticCount(
            """
            selectDeprecated:
            SELECT Deprecated
            FROM player;
            """.asSqlDelightFile(),
            0,
            options = mapOf("words" to "deprecated", "matchCase" to "true"),
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers by default`() {
        BlockedWordsRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- deprecated
            SELECT 'deprecated', "deprecated", `deprecated`, [deprecated]
            FROM player;
            """.asSqlDelightFile(),
            0,
            options = mapOf("words" to "deprecated"),
        )
    }

    @Test
    fun `can report blocked words in comments`() {
        BlockedWordsRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- deprecated
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
            1,
            options = mapOf("words" to "deprecated", "ignoreComments" to "false"),
        )
    }

    @Test
    fun `does not report when words option is empty`() {
        BlockedWordsRule().assertDiagnosticCount(
            """
            selectDeprecated:
            SELECT deprecated
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
