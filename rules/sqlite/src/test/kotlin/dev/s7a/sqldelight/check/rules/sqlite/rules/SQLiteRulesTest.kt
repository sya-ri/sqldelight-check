package dev.s7a.sqldelight.check.rules.sqlite.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SQLiteRulesTest {
    @Test
    fun `reports non integer primary key rowid declarations`() {
        PreferIntegerPrimaryKeyRule().assertOne("CREATE TABLE player (id INT PRIMARY KEY);")
        assertEquals(emptyList(), PreferIntegerPrimaryKeyRule().diagnostics("CREATE TABLE player (id INTEGER PRIMARY KEY);"))
    }

    @Test
    fun `reports autoincrement`() {
        NoAutoincrementWithoutNeedRule().assertOne("CREATE TABLE player (id INTEGER PRIMARY KEY AUTOINCREMENT);")
    }

    @Test
    fun `reports complex alter table changes`() {
        NoAlterTableComplexChangeRule().assertOne("ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name);")
    }

    @Test
    fun `reports composite primary key without rowid hint`() {
        PreferWithoutRowidForCompositePkRule().assertOne("CREATE TABLE player_score (player_id INTEGER, season INTEGER, PRIMARY KEY (player_id, season));")
        assertEquals(
            emptyList(),
            PreferWithoutRowidForCompositePkRule().diagnostics(
                "CREATE TABLE player_score (player_id INTEGER, season INTEGER, PRIMARY KEY (player_id, season)) WITHOUT ROWID;",
            ),
        )
    }
}
