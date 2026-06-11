package dev.s7a.sqldelight.check.rules.sqlite.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferWithoutRowidForCompositePkRuleTest {
    @Test
    fun `reports composite primary key without rowid hint`() {
        PreferWithoutRowidForCompositePkRule().assertOne(
            "CREATE TABLE player_score (player_id INTEGER, season INTEGER, PRIMARY KEY (player_id, season));",
        )
    }

    @Test
    fun `accepts composite primary key with without rowid`() {
        assertEquals(
            emptyList(),
            PreferWithoutRowidForCompositePkRule().diagnostics(
                "CREATE TABLE player_score (player_id INTEGER, season INTEGER, PRIMARY KEY (player_id, season)) WITHOUT ROWID;",
            ),
        )
    }
}
