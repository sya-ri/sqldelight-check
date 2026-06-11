package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class AvoidModelBoundInsertForPublicApiRuleTest {
    @Test
    fun `reports model-bound insert in sq files`() {
        val diagnostics =
            AvoidModelBoundInsertForPublicApiRule().diagnostics(
                """
                insertPlayer:
                INSERT INTO player
                VALUES ?;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts explicit insert values`() {
        AvoidModelBoundInsertForPublicApiRule().assertDiagnosticCount(
            """
            insertPlayer:
            INSERT INTO player(id, name)
            VALUES (:id, :name);
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores migration files and comments`() {
        AvoidModelBoundInsertForPublicApiRule().assertDiagnosticCount(
            """
            INSERT INTO player
            VALUES ?;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
        AvoidModelBoundInsertForPublicApiRule().assertDiagnosticCount(
            """
            insertPlayer:
            -- INSERT INTO player VALUES ?
            INSERT INTO player(id, name)
            VALUES (:id, :name);
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
