package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoWildcardImportRuleTest {
    @Test
    fun `reports wildcard imports`() {
        val diagnostics =
            NoWildcardImportRule().diagnostics(
                """
                import com.example.*;

                CREATE TABLE user (
                  id INTEGER NOT NULL
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }
}
