package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConstraintNewlineRuleTest {
    @Test
    fun `reports multiline table constraints sharing a line`() {
        val diagnostics =
            ConstraintNewlineRule().diagnostics(
                """
                CREATE TABLE player (
                  id INTEGER PRIMARY KEY,
                  name TEXT, UNIQUE (name)
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts table constraints on their own lines`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER PRIMARY KEY,
              name TEXT,
              UNIQUE (name)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports multiline column constraints sharing a line`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER
                PRIMARY KEY NOT NULL,
              name TEXT
            );
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts mapped type column constraints on their own lines`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE items (
              id UUID AS kotlin.uuid.Uuid NOT NULL PRIMARY KEY,
              name TEXT AS com.example.NonEmptyString NOT NULL
                CHECK (name <> '')
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts named column constraints on their own lines after mapped type`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE first_come_page_layout_file_references (
                file_id UUID AS kotlin.uuid.Uuid
                    CONSTRAINT fclfr_file_id_nn NOT NULL
                    CONSTRAINT fclfr_file_id_fk REFERENCES files (id)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts mapped type columns before table constraints`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE item_labels (
              item_id UUID AS kotlin.uuid.Uuid NOT NULL REFERENCES items (id) ON DELETE CASCADE,
              label TEXT AS com.example.NonEmptyString NOT NULL,
              PRIMARY KEY (item_id, label)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts split check constraints after normal column types`() {
        ConstraintNewlineRule().assertDiagnosticCount(
            """
            CREATE TABLE reports (
              id UUID AS kotlin.uuid.Uuid NOT NULL PRIMARY KEY,
              reason TEXT NOT NULL
                CHECK (reason IN ('A', 'B', 'C'))
            );
            """.asSqlDelightFile(),
            0,
        )
    }
}
