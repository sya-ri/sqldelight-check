package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class RequireWhereIndexFriendlyPredicateRuleTest {
    @Test
    fun `reports function-wrapped where predicates`() {
        val diagnostics =
            RequireWhereIndexFriendlyPredicateRule().diagnostics(
                """
                selectByName:
                SELECT id, name
                FROM player
                WHERE LOWER(name) = :name;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
    }

    @Test
    fun `accepts functions outside where comparisons`() {
        RequireWhereIndexFriendlyPredicateRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT LOWER(name) AS lower_name
            FROM player
            WHERE name = :name
            ORDER BY LOWER(name);
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
