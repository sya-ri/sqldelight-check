package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class MySqlRulesTest {
    @Test
    fun `reports utf8 charset`() {
        NoUtf8CharsetRule().assertOne("CREATE TABLE player (name TEXT) CHARSET=utf8;")
        assertEquals(emptyList(), NoUtf8CharsetRule().diagnostics("CREATE TABLE player (name TEXT) CHARSET=utf8mb4;"))
    }

    @Test
    fun `reports copy algorithm and exclusive lock`() {
        NoCopyAlgorithmRule().assertOne("ALTER TABLE player ADD COLUMN score INT, ALGORITHM=COPY;")
        NoExclusiveLockRule().assertOne("ALTER TABLE player ADD COLUMN score INT, LOCK=EXCLUSIVE;")
    }

    @Test
    fun `reports zero date defaults and display width integers`() {
        NoZeroDateDefaultRule().assertOne("CREATE TABLE event (created_at DATE DEFAULT '0000-00-00');")
        NoDisplayWidthIntegerRule().assertOne("CREATE TABLE player (score INT(11));")
    }

    @Test
    fun `reports text index without prefix length`() {
        RequireIndexPrefixLengthRule().assertOne(
            """
            CREATE TABLE player (name TEXT);
            CREATE INDEX player_name ON player(name);
            """,
        )
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
