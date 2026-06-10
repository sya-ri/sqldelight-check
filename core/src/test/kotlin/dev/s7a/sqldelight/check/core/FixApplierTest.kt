package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for safe text edit application.
 */
class FixApplierTest {
    @Test
    fun `applies safe fixes`() {
        val result =
            FixApplier().apply(
                content = "SELECT 1;",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 10, endColumn = 10),
                            replacement = "\n",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("SELECT 1;\n", result.content)
        assertEquals(1, result.appliedFixes)
    }

    @Test
    fun `skips unsafe fixes by default`() {
        val result =
            FixApplier().apply(
                content = "SELECT * FROM player;",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Unsafe,
                            range = range(line = 1, startColumn = 8, endColumn = 9),
                            replacement = "id",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("SELECT * FROM player;", result.content)
        assertEquals(0, result.appliedFixes)
        assertEquals(1, result.skippedFixes)
    }

    @Test
    fun `skips overlapping fixes`() {
        val result =
            FixApplier().apply(
                content = "abc",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 1, endColumn = 3),
                            replacement = "x",
                        ),
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 2, endColumn = 4),
                            replacement = "y",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("xc", result.content)
        assertEquals(1, result.appliedFixes)
        assertEquals(1, result.skippedFixes)
    }

    private fun diagnostic(
        safety: FixSafety,
        range: SourceRange,
        replacement: String,
    ): Diagnostic =
        Diagnostic(
            ruleId = RuleId("standard:test"),
            severity = Severity.Warning,
            message = "test",
            file = null,
            range = range,
            database = null,
            fixes =
                listOf(
                    Fix(
                        title = "test fix",
                        safety = safety,
                        edits = listOf(TextEdit(range = range, replacement = replacement)),
                    ),
                ),
        )

    private fun range(
        line: Int,
        startColumn: Int,
        endColumn: Int,
    ): SourceRange =
        SourceRange(
            start = SourcePosition(line = line, column = startColumn),
            end = SourcePosition(line = line, column = endColumn),
        )
}
