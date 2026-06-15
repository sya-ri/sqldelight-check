package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import kotlin.test.Test
import kotlin.test.assertEquals

class RequireConcurrentIndexRuleTest {
    @Test
    fun `reports non concurrent indexes`() {
        val diagnostics = RequireConcurrentIndexRule().diagnostics("CREATE INDEX player_name ON player(name);")

        assertEquals(1, diagnostics.size)
        assertEquals(qualifiedRuleId("postgres:require-concurrent-index"), diagnostics.single().ruleId)
    }

    @Test
    fun `accepts concurrent indexes and ignores comments`() {
        assertEquals(
            emptyList(),
            RequireConcurrentIndexRule().diagnostics(
                """
                -- CREATE INDEX player_name ON player(name);
                CREATE INDEX CONCURRENTLY player_name ON player(name);
                """,
            ),
        )
    }

    @Test
    fun `does not report non concurrent index for table created earlier in same migration`() {
        val diagnostics =
            RequireConcurrentIndexRule().diagnostics(
                migrationLocalCreateTableAndIndex,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `reports non concurrent index when table may already exist`() {
        val diagnostics =
            RequireConcurrentIndexRule().diagnostics(
                """
                CREATE TABLE IF NOT EXISTS items (
                    id UUID NOT NULL PRIMARY KEY
                );

                CREATE INDEX items_id_idx ON items (id);
                """,
            )

        assertEquals(1, diagnostics.size)
    }
}

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
