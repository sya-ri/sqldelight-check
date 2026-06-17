package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDuplicateImportRuleTest {
    @Test
    fun `reports duplicate imports`() {
        val diagnostics =
            NoDuplicateImportRule().diagnostics(
                """
                import com.example.UserType;
                import com.example.UserType;

                CREATE TABLE user (
                  type TEXT AS UserType NOT NULL
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoDuplicateImportRule().assertAllFixes(
            """
            import com.example.UserType;
            import com.example.UserType;

            CREATE TABLE user (
              type TEXT AS UserType NOT NULL
            );
            """.asSqlDelightFile(),
            """
            import com.example.UserType;

            CREATE TABLE user (
              type TEXT AS UserType NOT NULL
            );
            """.asSqlDelightFile(),
        )
    }
}
