package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
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
        assertEquals(0, result.skippedFixes)
        assertEquals(emptyList(), result.skippedFixDetails)
    }

    @Test
    fun `skips unsafe fixes by default with reason`() {
        val sourceFile = SourceFile(path = "src/main/sqldelight/test.sq", content = "SELECT * FROM player;")

        val result =
            FixApplier().apply(
                content = sourceFile.content,
                diagnostics =
                    listOf(
                        diagnostic(
                            file = sourceFile,
                            title = "replace star",
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
        assertEquals(
            listOf(
                SkippedFix(
                    ruleId = qualifiedRuleId("standard:test"),
                    file = sourceFile,
                    title = "replace star",
                    reason = FixSkipReason.Unsafe,
                ),
            ),
            result.skippedFixDetails,
        )
    }

    @Test
    fun `applies safe fix when unsafe candidate exists`() {
        val result =
            FixApplier().apply(
                content = "SELECT * FROM player;",
                diagnostics =
                    listOf(
                        diagnostic(
                            fixes =
                                listOf(
                                    fix(
                                        title = "replace star unsafely",
                                        safety = FixSafety.Unsafe,
                                        range = range(line = 1, startColumn = 8, endColumn = 9),
                                        replacement = "id",
                                    ),
                                    fix(
                                        title = "remove star safely",
                                        safety = FixSafety.Safe,
                                        range = range(line = 1, startColumn = 8, endColumn = 9),
                                        replacement = "name",
                                    ),
                                ),
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("SELECT name FROM player;", result.content)
        assertEquals(1, result.appliedFixes)
        assertEquals(0, result.skippedFixes)
    }

    @Test
    fun `skips invalid range fixes with reason`() {
        val result =
            FixApplier().apply(
                content = "SELECT 1;",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 2, startColumn = 1, endColumn = 2),
                            replacement = "x",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("SELECT 1;", result.content)
        assertEquals(0, result.appliedFixes)
        assertEquals(1, result.skippedFixes)
        assertEquals(FixSkipReason.InvalidRange, result.skippedFixDetails.single().reason)
    }

    @Test
    fun `skips fix with overlapping internal edits with reason`() {
        val result =
            FixApplier().apply(
                content = "abcdef",
                diagnostics =
                    listOf(
                        diagnostic(
                            fixes =
                                listOf(
                                    Fix(
                                        title = "overlapping edits",
                                        safety = FixSafety.Safe,
                                        edits =
                                            listOf(
                                                TextEdit(
                                                    range = range(line = 1, startColumn = 1, endColumn = 4),
                                                    replacement = "x",
                                                ),
                                                TextEdit(
                                                    range = range(line = 1, startColumn = 3, endColumn = 6),
                                                    replacement = "y",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("abcdef", result.content)
        assertEquals(0, result.appliedFixes)
        assertEquals(1, result.skippedFixes)
        assertEquals(FixSkipReason.OverlappingEdits, result.skippedFixDetails.single().reason)
    }

    @Test
    fun `skips overlapping diagnostic fix candidates with reason`() {
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
        assertEquals(FixSkipReason.OverlappingCandidate, result.skippedFixDetails.single().reason)
    }

    @Test
    fun `applies adjacent diagnostic fixes`() {
        val result =
            FixApplier().apply(
                content = "abcd",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 1, endColumn = 3),
                            replacement = "x",
                        ),
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 3, endColumn = 5),
                            replacement = "y",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("xy", result.content)
        assertEquals(2, result.appliedFixes)
        assertEquals(0, result.skippedFixes)
    }

    @Test
    fun `applies zero-length insert`() {
        val result =
            FixApplier().apply(
                content = "abc",
                diagnostics =
                    listOf(
                        diagnostic(
                            safety = FixSafety.Safe,
                            range = range(line = 1, startColumn = 2, endColumn = 2),
                            replacement = "X",
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("aXbc", result.content)
        assertEquals(1, result.appliedFixes)
        assertEquals(0, result.skippedFixes)
    }

    @Test
    fun `applies size-changing multi-edit fix`() {
        val result =
            FixApplier().apply(
                content = "aa bb cc",
                diagnostics =
                    listOf(
                        diagnostic(
                            fixes =
                                listOf(
                                    Fix(
                                        title = "size changing edits",
                                        safety = FixSafety.Safe,
                                        edits =
                                            listOf(
                                                TextEdit(
                                                    range = range(line = 1, startColumn = 1, endColumn = 3),
                                                    replacement = "alpha",
                                                ),
                                                TextEdit(
                                                    range = range(line = 1, startColumn = 4, endColumn = 6),
                                                    replacement = "b",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                allowUnsafe = false,
            )

        assertEquals("alpha b cc", result.content)
        assertEquals(1, result.appliedFixes)
        assertEquals(0, result.skippedFixes)
    }

    private fun diagnostic(
        file: SourceFile? = null,
        title: String = "test fix",
        safety: FixSafety,
        range: SourceRange,
        replacement: String,
    ): Diagnostic {
        val ruleId = qualifiedRuleId("standard:test")
        return Diagnostic(
            ruleId = ruleId,
            severity = Severity.Warning,
            message = "test",
            file = file,
            range = range,
            database = null,
            fixes =
                listOf(
                    fix(title = title, safety = safety, range = range, replacement = replacement),
                ),
        )
    }

    private fun diagnostic(fixes: List<Fix>): Diagnostic {
        val ruleId = qualifiedRuleId("standard:test")
        return Diagnostic(
            ruleId = ruleId,
            severity = Severity.Warning,
            message = "test",
            file = null,
            range = null,
            database = null,
            fixes = fixes,
        )
    }

    private fun fix(
        title: String,
        safety: FixSafety,
        range: SourceRange,
        replacement: String,
    ): Fix =
        Fix(
            title = title,
            safety = safety,
            edits = listOf(TextEdit(range = range, replacement = replacement)),
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

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
