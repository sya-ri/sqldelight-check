package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentParameterNamesRuleTest {
    @Test
    fun `reports repeated column predicates with different named parameters`() {
        val diagnostics =
            ConsistentParameterNamesRule().diagnostics(
                """
                selectByName:
                SELECT id, name
                FROM player
                WHERE name = :name
                   OR name = :otherName;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts repeated column predicates with same named parameter`() {
        ConsistentParameterNamesRule().assertDiagnosticCount(
            """
            selectByName:
            SELECT id, name
            FROM player
            WHERE name = :name
               OR name = :name;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores migration files and anonymous parameters`() {
        ConsistentParameterNamesRule().assertDiagnosticCount(
            """
            SELECT id FROM player WHERE name = :name OR name = :otherName;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
        ConsistentParameterNamesRule().assertDiagnosticCount(
            """
            selectByName:
            SELECT id FROM player WHERE name = ? OR name = ?;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `does not compare predicates across separate statements`() {
        ConsistentParameterNamesRule().assertDiagnosticCount(
            """
            selectByName:
            SELECT id FROM player WHERE name = :name;

            selectByOtherName:
            SELECT id FROM player WHERE name = :otherName;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
