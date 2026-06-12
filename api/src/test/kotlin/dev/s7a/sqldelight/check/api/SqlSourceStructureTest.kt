package dev.s7a.sqldelight.check.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlSourceStructureTest {
    @Test
    fun `tracks parenthesis nesting for table constraints`() {
        val structure =
            SqlSourceStructure.parse(
                """
                CREATE TABLE sample (
                    id uuid NOT NULL PRIMARY KEY,
                    reason TEXT NOT NULL,
                    detail TEXT,
                    CHECK (
                        (reason = 'OTHER' AND detail IS NOT NULL AND detail != '')
                            OR (reason != 'OTHER' AND detail IS NULL)
                    )
                );
                """.trimIndent(),
            )

        assertEquals(1, structure.context("CHECK").parenthesisDepth)
        assertEquals(3, structure.context("reason", occurrence = 2).parenthesisDepth)
        assertEquals(2, structure.context("OR").parenthesisDepth)
    }

    @Test
    fun `tracks case nesting independently from parenthesis nesting`() {
        val structure =
            SqlSourceStructure.parse(
                """
                SELECT CASE
                    WHEN EXISTS (SELECT 1 FROM sample) THEN 1
                    ELSE 0
                END;
                SELECT 2;
                """.trimIndent(),
            )

        assertEquals(1, structure.context("WHEN").caseDepth)
        assertEquals(1, structure.context("SELECT", occurrence = 2).parenthesisDepth)
        assertEquals(1, structure.context("SELECT", occurrence = 3).statementIndex)
    }

    @Test
    fun `ignores comments strings quoted identifiers and dollar quoted strings`() {
        val structure =
            SqlSourceStructure.parse(
                """
                SELECT '(' AS value, ${'$'}body${'$'});${'$'}body${'$'} AS body -- );
                FROM [odd;name];
                SELECT 1;
                """.trimIndent(),
            )

        assertEquals(0, structure.context("FROM").statementIndex)
        assertEquals(0, structure.context("FROM").parenthesisDepth)
        assertEquals(1, structure.context("SELECT", occurrence = 2).statementIndex)
    }

    @Test
    fun `matches dialect source pattern roles at token nesting`() {
        val patterns =
            SqlDialectSourcePatterns(
                patterns =
                    SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                        sourcePatterns(
                            "QUALIFY",
                            roles = setOf(SqlDialectSourcePatternRole.ClauseBoundary),
                        ),
            )
        val structure =
            SqlSourceStructure.parse(
                source = "SELECT * FROM sample QUALIFY row_number() = 1",
                sourcePatterns = patterns,
            )
        val qualify = structure.context("QUALIFY")

        assertEquals(0, qualify.parenthesisDepth)
        assertTrue(qualify.matches(SqlDialectSourcePatternRole.ClauseBoundary))
        assertEquals(1, qualify.matchLength(SqlDialectSourcePatternRole.ClauseBoundary))
    }

    @Test
    fun `keeps dialect specific multi term pattern lengths`() {
        val structure =
            SqlSourceStructure.parse(
                source = "SELECT * FROM sample ORDER BY id FETCH FIRST ROWS ONLY",
                sourcePatterns = SqlDialectSourcePatterns.PostgreSql,
            )
        val fetch = structure.context("FETCH")

        assertTrue(fetch.matches(SqlDialectSourcePatternRole.ClauseBoundary))
        assertEquals(3, fetch.matchLength(SqlDialectSourcePatternRole.ClauseBoundary))
    }

    private fun SqlSourceStructure.context(
        text: String,
        occurrence: Int = 1,
    ): SqlSourceTokenContext {
        val context =
            tokens
                .filter { context -> context.token.text.equals(text, ignoreCase = true) }
                .drop(occurrence - 1)
                .firstOrNull()
        return assertNotNull(context, "Expected token $text occurrence $occurrence")
    }
}
