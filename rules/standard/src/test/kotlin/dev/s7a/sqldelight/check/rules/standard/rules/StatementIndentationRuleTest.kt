package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.TextEdit
import kotlin.test.Test
import kotlin.test.assertEquals

class StatementIndentationRuleTest {
    @Test
    fun `formats complex cte select query indentation`() {
        val input =
            """
            selectAccountDashboard:
                  WITH recent_orders AS (
            SELECT
             o.id,
                    o.account_id,
               SUM(oi.quantity * oi.price) AS total
            FROM orders AS o
                   JOIN order_items AS oi ON oi.order_id = o.id
              WHERE o.created_at >= :since
            AND (
            o.status = 'PAID'
                  OR o.status = 'SHIPPED'
            )
                  GROUP BY o.id, o.account_id
            ),
                  ranked_orders AS (
                 SELECT
            account_id,
                         total,
             ROW_NUMBER() OVER (
            PARTITION BY account_id
              ORDER BY total DESC
                  ) AS row_number
               FROM recent_orders
            )
             SELECT
             a.id,
                    a.name,
              ro.total
            FROM accounts AS a
                  JOIN ranked_orders AS ro ON ro.account_id = a.id
                  WHERE ro.row_number = 1
              AND EXISTS (
            SELECT 1
               FROM account_notes AS n
                  WHERE n.account_id = a.id
            AND n.archived = 0
              )
                  ORDER BY ro.total DESC;
            """.asSqlDelightFile()

        val expected =
            """
            selectAccountDashboard:
            WITH recent_orders AS (
              SELECT
                o.id,
                o.account_id,
                SUM(oi.quantity * oi.price) AS total
              FROM orders AS o
              JOIN order_items AS oi ON oi.order_id = o.id
              WHERE o.created_at >= :since
                AND (
                  o.status = 'PAID'
                  OR o.status = 'SHIPPED'
                )
              GROUP BY o.id, o.account_id
            ),
            ranked_orders AS (
              SELECT
                account_id,
                total,
                ROW_NUMBER() OVER (
                  PARTITION BY account_id
                  ORDER BY total DESC
                ) AS row_number
              FROM recent_orders
            )
            SELECT
              a.id,
              a.name,
              ro.total
            FROM accounts AS a
            JOIN ranked_orders AS ro ON ro.account_id = a.id
            WHERE ro.row_number = 1
              AND EXISTS (
                SELECT 1
                FROM account_notes AS n
                WHERE n.account_id = a.id
                  AND n.archived = 0
              )
            ORDER BY ro.total DESC;
            """.asSqlDelightFile()

        assertEquals(expected, StatementIndentationRule().applyAllFixes(input))
    }

    @Test
    fun `formats mixed insert update and table constraint indentation`() {
        val input =
            """
            CREATE TABLE sample (
                 id INTEGER NOT NULL PRIMARY KEY,
            reason TEXT NOT NULL,
                    detail TEXT,
              CHECK (
            (reason = 'OTHER' AND detail IS NOT NULL AND detail != '')
                 OR (reason != 'OTHER' AND detail IS NULL)
              )
            );

            upsertSample:
                  INSERT INTO sample(
            id,
               reason,
                    detail
            )
             VALUES (
            :id,
                    :reason,
               :detail
            )
             ON CONFLICT(id) DO UPDATE
                    SET reason = excluded.reason,
              detail = excluded.detail
            WHERE excluded.reason = 'OTHER'
                 OR sample.detail IS NOT NULL;
            """.asSqlDelightFile()

        val expected =
            """
            CREATE TABLE sample (
              id INTEGER NOT NULL PRIMARY KEY,
              reason TEXT NOT NULL,
              detail TEXT,
              CHECK (
                (reason = 'OTHER' AND detail IS NOT NULL AND detail != '')
                OR (reason != 'OTHER' AND detail IS NULL)
              )
            );

            upsertSample:
            INSERT INTO sample(
              id,
              reason,
              detail
            )
            VALUES (
              :id,
              :reason,
              :detail
            )
            ON CONFLICT(id) DO UPDATE
            SET reason = excluded.reason,
            detail = excluded.detail
            WHERE excluded.reason = 'OTHER'
              OR sample.detail IS NOT NULL;
            """.asSqlDelightFile()

        assertEquals(expected, StatementIndentationRule().applyAllFixes(input))
    }

    @Test
    fun `accepts already formatted query`() {
        StatementIndentationRule().assertDiagnosticCount(
            """
            selectByName:
            SELECT
              id,
              name
            FROM player
            WHERE name = :name
              OR nickname = :name;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores one line query`() {
        StatementIndentationRule().assertDiagnosticCount(
            """
            selectByName:
                SELECT id, name FROM player WHERE name = :name;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `formats custom dialect query indentation without dialect parser support`() {
        val input =
            """
            selectVectorMatches:
                    WITH matched AS (
              SELECT
            vector_distance_cosine(embedding, :embedding) AS distance,
                         metadata->>'title' AS title
                FROM documents
                  WHERE metadata @? '$.tags[*] ? (@ == "release")'
            AND (
                       tenant_id = :tenantId
              OR visibility = 'public'
            )
            )
                 SELECT
            title,
                   distance
             FROM matched
                  ORDER BY distance ASC;
            """.asSqlDelightFile()

        val expected =
            """
            selectVectorMatches:
            WITH matched AS (
              SELECT
                vector_distance_cosine(embedding, :embedding) AS distance,
                metadata->>'title' AS title
              FROM documents
              WHERE metadata @? '$.tags[*] ? (@ == "release")'
                AND (
                  tenant_id = :tenantId
                  OR visibility = 'public'
                )
            )
            SELECT
              title,
              distance
            FROM matched
            ORDER BY distance ASC;
            """.asSqlDelightFile()

        assertEquals(
            expected,
            StatementIndentationRule().applyAllFixes(
                input,
                dialect = SqlDialect(family = DialectFamily.Custom),
            ),
        )
    }

    @Test
    fun `reports safe indentation fixes`() {
        val diagnostics =
            StatementIndentationRule().diagnostics(
                """
                selectAll:
                    SELECT id
                  FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(true, diagnostics.all { diagnostic -> diagnostic.fixes.single().safety == FixSafety.Safe })
    }
}

private fun StatementIndentationRule.applyAllFixes(
    content: String,
    dialect: SqlDialect = SqlDialect(family = DialectFamily.SQLite),
): String {
    var current = content
    repeat(10) {
        val fixes =
            diagnostics(current, dialect = dialect)
                .flatMap { diagnostic -> diagnostic.fixes }
                .filter { fix -> fix.safety == FixSafety.Safe }
                .flatMap { fix -> fix.edits }
        if (fixes.isEmpty()) return current
        current = current.applyEdits(fixes)
    }
    return current
}

private fun String.applyEdits(edits: List<TextEdit>): String {
    val builder = StringBuilder(this)
    edits
        .map { edit ->
            OffsetEdit(
                start = offsetAt(edit.range.start),
                end = offsetAt(edit.range.end),
                replacement = edit.replacement,
            )
        }
        .sortedByDescending { edit -> edit.start }
        .forEach { edit -> builder.replace(edit.start, edit.end, edit.replacement) }
    return builder.toString()
}

private data class OffsetEdit(
    val start: Int,
    val end: Int,
    val replacement: String,
)

private fun String.offsetAt(position: SourcePosition): Int {
    var line = 1
    var column = 1
    for (index in indices) {
        if (line == position.line && column == position.column) return index
        if (this[index] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return length
}
