package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class MappedTypeNameCaseRuleTest {
    @Test
    fun `reports mapped type names that are not upper camel`() {
        val content =
            """
            CREATE TABLE user (
              enabled INTEGER AS boolean NOT NULL
            );
            """.asSqlDelightFile()
        val diagnostics =
            MappedTypeNameCaseRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
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
