package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceIndentationRuleTest {
    @Test
    fun `fixes indentation for complex multiline query layout`() {
        val input =
            """
            selectComplex:
            SELECT id,
            name,
              CASE
            WHEN EXISTS (
            SELECT 1
               FROM orders
            WHERE orders.account_id = player.id
            AND orders.deleted_at IS NULL
            ) THEN 'active'
            ELSE 'inactive'
            END AS state
              FROM player
            WHERE active = 1
            AND (
            score > 10
             OR score IS NULL
            )
               ORDER BY name,
            id;
            """.asSqlDelightFile()

        val expected =
            """
            selectComplex:
            SELECT id,
                name,
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM orders
                        WHERE orders.account_id = player.id
                            AND orders.deleted_at IS NULL
                    ) THEN 'active'
                    ELSE 'inactive'
                END AS state
            FROM player
            WHERE active = 1
                AND (
                    score > 10
                    OR score IS NULL
                )
            ORDER BY name,
                id;
            """.asSqlDelightFile()

        assertEquals(expected, SourceIndentationRule().applyAllFixes(input))
    }

    @Test
    fun `accepts one line statements`() {
        SourceIndentationRule().assertDiagnosticCount("SELECT id FROM player WHERE id = ?;", 0)
    }

    @Test
    fun `uses configured indentation size`() {
        val input =
            """
            SELECT id,
            name
            FROM player
            WHERE active = 1
            AND (
            score > 10
            OR score IS NULL
            );
            """.asSqlDelightFile()

        val expected =
            """
            SELECT id,
              name
            FROM player
            WHERE active = 1
              AND (
                score > 10
                OR score IS NULL
              );
            """.asSqlDelightFile()

        assertEquals(expected, SourceIndentationRule().applyAllFixes(input, options = mapOf("indentSize" to "2")))
    }

    @Test
    fun `aligns query body from the file base indentation`() {
        val input =
            """
            selectById:
              SELECT id,
            name
              FROM player
            """.asSqlDelightFile()

        val expected =
            """
            selectById:
            SELECT id,
                name
            FROM player
            """.asSqlDelightFile()

        assertEquals(expected, SourceIndentationRule().applyAllFixes(input))
    }

    @Test
    fun `fixes table and check constraint indentation`() {
        val input =
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
            """.asSqlDelightFile()

        val expected =
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
            """.asSqlDelightFile()

        assertEquals(expected, SourceIndentationRule().applyAllFixes(input))
    }

    @Test
    fun `accepts multiline insert column and values lists`() {
        val input =
            """
            insertItem:
            INSERT INTO items (
                id,
                name,
                description
            )
            VALUES (
                :id,
                :name,
                :description
            );
            """.asSqlDelightFile()

        SourceIndentationRule().assertDiagnosticCount(input, 0)
    }

    @Test
    fun `accepts multiline insert with on conflict`() {
        val input =
            """
            insertItem:
            INSERT INTO items (
                id,
                name,
                description
            )
            VALUES (
                :id,
                :name,
                :description
            )
            ON CONFLICT (id) DO NOTHING;
            """.asSqlDelightFile()

        SourceIndentationRule().assertDiagnosticCount(input, 0)
    }

    @Test
    fun `accepts indented create index target table line`() {
        val input =
            """
            CREATE TABLE sample_items (
                id UUID NOT NULL PRIMARY KEY,
                owner_id TEXT NOT NULL,
                name TEXT AS com.example.NonEmptyString NOT NULL
                    CHECK (name != ''),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            CREATE INDEX sample_items_owner_created_at_idx
                ON sample_items (owner_id, created_at DESC);
            """.asSqlDelightFile()

        SourceIndentationRule().assertDiagnosticCount(input, 0)
    }

    @Test
    fun `accepts mapped types in create table constraints`() {
        val input =
            """
            CREATE TABLE items (
                id UUID AS kotlin.uuid.Uuid NOT NULL PRIMARY KEY,
                name TEXT AS com.example.NonEmptyString NOT NULL,
                PRIMARY KEY (id, name)
            );
            """.asSqlDelightFile()

        SourceIndentationRule().assertDiagnosticCount(input, 0)
    }

    @Test
    fun `accepts mapped types with multiline check constraints`() {
        val input =
            """
            CREATE TABLE items (
                id UUID AS kotlin.uuid.Uuid NOT NULL PRIMARY KEY,
                name TEXT AS com.example.NonEmptyString NOT NULL
                    CHECK (name <> '')
            );
            """.asSqlDelightFile()

        SourceIndentationRule().assertDiagnosticCount(input, 0)
    }
}
