package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireNotValidConstraintRuleTest {
    @Test
    fun `reports add constraint without not valid`() {
        RequireNotValidConstraintRule().assertOne(
            "ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name);",
        )
    }

    @Test
    fun `accepts add constraint not valid`() {
        assertEquals(
            emptyList(),
            RequireNotValidConstraintRule().diagnostics(
                "ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name) NOT VALID;",
            ),
        )
    }
}
