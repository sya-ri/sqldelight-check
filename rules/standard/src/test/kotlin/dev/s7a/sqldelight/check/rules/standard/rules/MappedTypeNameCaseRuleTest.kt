package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class MappedTypeNameCaseRuleTest {
    @Test
    fun `reports mapped type names that are not upper camel`() {
        val diagnostics =
            MappedTypeNameCaseRule().diagnostics(
                """
                CREATE TABLE user (
                  enabled INTEGER AS boolean NOT NULL
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts upper camel mapped type names`() {
        MappedTypeNameCaseRule().assertDiagnosticCount(
            """
            CREATE TABLE user (
              enabled INTEGER AS Boolean NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
