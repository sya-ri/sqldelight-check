package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportOrderRuleTest {
    @Test
    fun `reports and fixes unsorted imports`() {
        val diagnostics =
            ImportOrderRule().diagnostics(
                """
                import com.example.UserType;
                import kotlin.time.Instant;
                import java.math.BigDecimal;

                CREATE TABLE event (
                  id INTEGER NOT NULL
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(
            """
            import kotlin.time.Instant;
            import java.math.BigDecimal;
            import com.example.UserType;
            """.trimIndent() + "\n",
            diagnostics.single().fixes.single().edits.single().replacement,
        )
    }

    @Test
    fun `accepts sorted imports and ignores migrations`() {
        val content =
            """
            import kotlin.time.Instant;
            import com.example.UserType;

            CREATE TABLE event (
              id INTEGER NOT NULL
            );
            """.asSqlDelightFile()

        ImportOrderRule().assertDiagnosticCount(content, 0)
        ImportOrderRule().assertDiagnosticCount(content, 0, path = MIGRATION_SQM_PATH)
    }
}
