package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.rule.api.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresMigrationSafetyRulesTest {
    @Test
    fun `require concurrent index reports non concurrent indexes`() {
        val diagnostics = RequireConcurrentIndexRule().diagnostics("CREATE INDEX player_name ON player(name);")

        assertEquals(1, diagnostics.size)
        assertEquals(RuleId("postgres:require-concurrent-index"), diagnostics.single().ruleId)
    }

    @Test
    fun `require concurrent index accepts concurrent indexes and ignores comments`() {
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
    fun `reports concurrent index in transaction`() {
        NoConcurrentIndexInTransactionRule().assertOne(
            """
            BEGIN;
            CREATE INDEX CONCURRENTLY player_name ON player(name);
            COMMIT;
            """,
        )
    }

    @Test
    fun `reports add constraint without not valid`() {
        RequireNotValidConstraintRule().assertOne(
            "ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name);",
        )
        assertEquals(
            emptyList(),
            RequireNotValidConstraintRule().diagnostics(
                "ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name) NOT VALID;",
            ),
        )
    }

    @Test
    fun `reports set not null on existing column`() {
        NoSetNotNullOnExistingColumnRule().assertOne(
            "ALTER TABLE player ALTER COLUMN name SET NOT NULL;",
        )
    }

    @Test
    fun `reports add column with volatile default`() {
        NoAddColumnWithVolatileDefaultRule().assertOne(
            "ALTER TABLE player ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();",
        )
    }

    @Test
    fun `reports serial types`() {
        PreferIdentityOverSerialRule().assertOne("CREATE TABLE player (id BIGSERIAL PRIMARY KEY);")
    }

    @Test
    fun `reports destructive rename and drop operations`() {
        NoDropColumnRule().assertOne("ALTER TABLE player DROP COLUMN old_name;")
        NoRenameColumnRule().assertOne("ALTER TABLE player RENAME COLUMN name TO display_name;")
        NoRenameTableRule().assertOne("ALTER TABLE player RENAME TO players;")
    }
}

private fun Rule.assertOne(content: String) {
    assertEquals(1, diagnostics(content).size)
}
