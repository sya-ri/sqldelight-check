package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConstraintNewlineRuleTest {
    @Test
    fun `reports multiline table constraints sharing a line`() {
        val diagnostics =
            ConstraintNewlineRule().diagnostics(
                """
                CREATE TABLE player (
                  id INTEGER PRIMARY KEY,
                  name TEXT, UNIQUE (name)
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts table constraints on their own lines`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER PRIMARY KEY,
              name TEXT,
              UNIQUE (name)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports multiline column constraints sharing a line`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER
                PRIMARY KEY NOT NULL,
              name TEXT
            );
            """.asSqlDelightFile(),
            1,
        )
    }
}
