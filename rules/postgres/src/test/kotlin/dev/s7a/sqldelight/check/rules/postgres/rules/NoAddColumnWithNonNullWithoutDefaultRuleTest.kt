package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoAddColumnWithNonNullWithoutDefaultRuleTest {
    @Test
    fun `reports add column not null without default`() {
        NoAddColumnWithNonNullWithoutDefaultRule().assertOne(
            """
            ALTER TABLE player ADD COLUMN score INTEGER NOT NULL;
            """,
        )
    }

    @Test
    fun `ignores add column not null with default`() {
        assertEquals(
            emptyList(),
            NoAddColumnWithNonNullWithoutDefaultRule().diagnostics(
                """
                ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
                """,
            ),
        )
    }

    @Test
    fun `ignores nullable add column`() {
        assertEquals(
            emptyList(),
            NoAddColumnWithNonNullWithoutDefaultRule().diagnostics(
                """
                ALTER TABLE player ADD COLUMN score INTEGER;
                """,
            ),
        )
    }
}
