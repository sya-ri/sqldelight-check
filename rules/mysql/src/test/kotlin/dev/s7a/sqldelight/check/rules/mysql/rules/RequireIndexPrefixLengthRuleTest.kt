package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireIndexPrefixLengthRuleTest {
    @Test
    fun `reports text index without prefix length`() {
        RequireIndexPrefixLengthRule().assertOne(
            """
            CREATE TABLE player (name TEXT);
            CREATE INDEX player_name ON player(name);
            """,
        )
    }

    @Test
    fun `accepts text index with prefix length`() {
        assertEquals(
            emptyList(),
            RequireIndexPrefixLengthRule().diagnostics(
                """
                CREATE TABLE player (name TEXT);
                CREATE INDEX player_name ON player(name(32));
                """,
            ),
        )
    }
}
