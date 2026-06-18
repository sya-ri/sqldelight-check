package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NoCompositePrimaryKeyRuleTest {
    @Test
    fun `is disabled by default`() {
        assertFalse(NoCompositePrimaryKeyRule().defaultEnable)
    }

    @Test
    fun `reports composite primary key table constraints`() {
        val diagnostics =
            NoCompositePrimaryKeyRule().diagnostics(
                """
                CREATE TABLE item_labels (
                  item_id INTEGER NOT NULL,
                  label TEXT NOT NULL,
                  PRIMARY KEY (item_id, label)
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Avoid composite primary keys and use a single-column primary key.", diagnostics.single().message)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts single column table primary key constraints`() {
        NoCompositePrimaryKeyRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL,
              name TEXT NOT NULL,
              PRIMARY KEY (id)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts column primary key constraints`() {
        NoCompositePrimaryKeyRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores primary key text outside create table definitions`() {
        NoCompositePrimaryKeyRule().assertDiagnosticCount(
            """
            -- PRIMARY KEY (id, name)
            selectPrimaryKey:
            SELECT 'PRIMARY KEY (id, name)';
            """.asSqlDelightFile(),
            0,
        )
    }
}
