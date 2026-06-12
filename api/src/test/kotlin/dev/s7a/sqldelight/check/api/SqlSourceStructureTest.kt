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
    fun `does not match SQLDelight named parameters as clause starts`() {
        val structure =
            SqlSourceStructure.parse(
                """
                selectPlayers:
                SELECT id, name
                FROM player
                LIMIT :limit
                OFFSET :offset;
                """.trimIndent(),
            )

        assertEquals(1, structure.context("LIMIT").matchLength(SqlDialectSourcePatternRole.MajorClauseStart))
        assertEquals(1, structure.context("OFFSET").matchLength(SqlDialectSourcePatternRole.MajorClauseStart))
        assertEquals(1, structure.tokens.count { context -> context.token.text.equals("limit", ignoreCase = true) })
        assertEquals(1, structure.tokens.count { context -> context.token.text.equals("offset", ignoreCase = true) })
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

    @Test
    fun `builds statement clause and subquery blocks`() {
        val structure =
            SqlSourceStructure.parse(
                """
                SELECT account.id
                FROM account
                WHERE EXISTS (
                    SELECT 1
                    FROM orders
                    WHERE orders.account_id = account.id
                )
                ORDER BY account.id;
                """.trimIndent(),
            )
        val statement = structure.blockStartingAt(SqlSourceBlockKind.Statement, "SELECT")
        val outerWhere = structure.blockStartingAt(SqlSourceBlockKind.Clause, "WHERE")
        val subquery = structure.blockStartingAt(SqlSourceBlockKind.Subquery, "(")
        val innerSelect = structure.blockStartingAt(SqlSourceBlockKind.Clause, "SELECT", occurrence = 2)

        assertEquals(structure.blocks.indexOf(statement), structure.blockStartingAt(SqlSourceBlockKind.Clause, "SELECT").parentBlockIndex)
        assertEquals(structure.blocks.indexOf(outerWhere), subquery.parentBlockIndex)
        assertEquals(structure.blocks.indexOf(subquery), innerSelect.parentBlockIndex)
        assertEquals(
            0,
            structure.blocks.count { block ->
                block.kind == SqlSourceBlockKind.Clause &&
                    structure.tokens[block.startTokenIndex].token.text.equals("BY", ignoreCase = true)
            },
        )
    }

    @Test
    fun `builds case expression blocks inside clauses`() {
        val structure =
            SqlSourceStructure.parse(
                """
                SELECT CASE
                    WHEN status = 'A' THEN 1
                    ELSE 0
                END AS rank
                FROM sample;
                """.trimIndent(),
            )
        val select = structure.blockStartingAt(SqlSourceBlockKind.Clause, "SELECT")
        val case = structure.blockStartingAt(SqlSourceBlockKind.CaseExpression, "CASE")
        val whenContext = structure.context("WHEN")

        assertEquals(structure.blocks.indexOf(select), case.parentBlockIndex)
        assertEquals(case, structure.innermostBlockContaining(whenContext))
        assertEquals(listOf("CASE", "WHEN", "status"), structure.tokensInBlock(case).take(3).map { context -> context.token.text })
    }

    @Test
    fun `uses custom dialect clause patterns for blocks`() {
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
        val qualify = structure.blockStartingAt(SqlSourceBlockKind.Clause, "QUALIFY")

        assertEquals(1, qualify.sourcePatternMatch?.length)
        assertTrue(SqlDialectSourcePatternRole.ClauseBoundary in qualify.sourcePatternMatch?.roles.orEmpty())
    }

    @Test
    fun `uses injected dialect block patterns for nesting and blocks`() {
        val patterns =
            SqlDialectSourcePatterns(
                blockPatterns =
                    SqlDialectSourceBlockPatterns(
                        statementSeparatorTerms = setOf("@"),
                        parenthesisDepthTerms =
                            setOf(
                                SqlDialectSourceParenthesisDepthTerms(
                                    openTerm = "{",
                                    closeTerm = "}",
                                ),
                            ),
                        pairedBlocks =
                            setOf(
                                SqlDialectSourcePairedBlockPattern.parse(
                                    startExpression = "BEGIN ATOMIC",
                                    endExpression = "END",
                                    kind = SqlSourceBlockKind.CaseExpression,
                                ),
                            ),
                        parenthesizedBlocks =
                            setOf(
                                SqlDialectSourceParenthesizedBlockPattern(
                                    openTerm = "{",
                                    closeTerm = "}",
                                    defaultKind = SqlSourceBlockKind.ParenthesizedExpression,
                                    innerStartRoles = setOf(SqlDialectSourcePatternRole.SelectListStart),
                                    innerStartKind = SqlSourceBlockKind.Subquery,
                                ),
                            ),
                    ),
            )
        val structure =
            SqlSourceStructure.parse(
                source = "SELECT { SELECT BEGIN ATOMIC WHEN ready THEN 1 END } @ SELECT 2",
                sourcePatterns = patterns,
            )
        val subquery = structure.blockStartingAt(SqlSourceBlockKind.Subquery, "{")
        val injectedCase = structure.blockStartingAt(SqlSourceBlockKind.CaseExpression, "BEGIN")

        assertEquals(1, structure.context("SELECT", occurrence = 2).parenthesisDepth)
        assertEquals(1, structure.context("WHEN").caseDepth)
        assertEquals(1, structure.context("SELECT", occurrence = 3).statementIndex)
        assertEquals(structure.blocks.indexOf(subquery), injectedCase.parentBlockIndex)
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

    private fun SqlSourceStructure.blockStartingAt(
        kind: SqlSourceBlockKind,
        text: String,
        occurrence: Int = 1,
    ): SqlSourceBlock {
        var seen = 0
        blocks.forEach { block ->
            if (block.kind == kind && tokens[block.startTokenIndex].token.text.equals(text, ignoreCase = true)) {
                seen++
                if (seen == occurrence) {
                    return block
                }
            }
        }
        return assertNotNull(null, "Expected $kind block at $text")
    }
}
