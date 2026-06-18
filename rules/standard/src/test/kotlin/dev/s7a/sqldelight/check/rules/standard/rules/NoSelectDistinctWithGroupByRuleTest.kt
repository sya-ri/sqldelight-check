package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelectDistinctWithGroupByRuleTest {
    @Test
    fun `reports select distinct with group by`() {
        val content =
            """
            selectDistinctNames:
            SELECT DISTINCT name
            FROM player
            GROUP BY name;
            """.asSqlDelightFile()
        val diagnostics = NoSelectDistinctWithGroupByRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        NoSelectDistinctWithGroupByRule().assertAllFixes(
            content,
            """
            selectDistinctNames:
            SELECT name
            FROM player
            GROUP BY name;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports select distinct with group by in migration files`() {
        val diagnostics =
            NoSelectDistinctWithGroupByRule().diagnostics(
                """
                INSERT INTO player_name_snapshot(name)
                SELECT DISTINCT name
                FROM player
                GROUP BY name;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts distinct without group by and group by without distinct`() {
        NoSelectDistinctWithGroupByRule().assertDiagnosticCount(
            """
            selectDistinctNames:
            SELECT DISTINCT name
            FROM player;

            selectGroupedNames:
            SELECT name
            FROM player
            GROUP BY name;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoSelectDistinctWithGroupByRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT DISTINCT name FROM player GROUP BY name
            SELECT 'DISTINCT GROUP BY', "DISTINCT", `GROUP`, [GROUP BY]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
