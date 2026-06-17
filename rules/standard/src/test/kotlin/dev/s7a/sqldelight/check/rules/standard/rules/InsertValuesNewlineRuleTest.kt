package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertValuesNewlineRuleTest {
    @Test
    fun `reports multiline insert column list with multiple items on one line`() {
        val content =
            """
            INSERT INTO player (id, name,
              age)
            VALUES (1, 'Ada', 42);
            """.asSqlDelightFile()
        val diagnostics = InsertValuesNewlineRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        InsertValuesNewlineRule().assertAllFixes(
            content,
            """
            INSERT INTO player (
            id,
            name,
              age)
            VALUES (1, 'Ada', 42);
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports multiline values list with multiple items on one line`() {
        InsertValuesNewlineRule().assertDiagnosticCount(
            """
            INSERT INTO player (id, name, age)
            VALUES (1, 'Ada',
              42);
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts matching multiline insert lists`() {
        InsertValuesNewlineRule().assertDiagnosticCount(
            """
            INSERT INTO player (
              id,
              name,
              age
            )
            VALUES (
              1,
              'Ada',
              42
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line insert values`() {
        InsertValuesNewlineRule().assertDiagnosticCount(
            """
            INSERT INTO player (id, name, age) VALUES (1, 'Ada', 42);
            """.asSqlDelightFile(),
            0,
        )
    }
}
