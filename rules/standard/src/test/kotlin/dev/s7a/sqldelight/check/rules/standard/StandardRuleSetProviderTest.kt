package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the standard rule set.
 */
class StandardRuleSetProviderTest {
    @Test
    fun `standard rule set provides built in style rules`() {
        val ruleIds =
            StandardRuleSetProvider()
                .ruleProviders()
                .map { provider -> provider.create().id }
                .toSet()

        assertEquals(
            setOf(
                RuleId("standard:consistent-column-references"),
                RuleId("standard:consistent-not-equal-operator"),
                RuleId("standard:consistent-order-by-direction"),
                RuleId("standard:data-type-case"),
                RuleId("standard:explicit-cross-join"),
                RuleId("standard:explicit-inner-join"),
                RuleId("standard:explicit-union-operator"),
                RuleId("standard:final-newline"),
                RuleId("standard:function-name-case"),
                RuleId("standard:keyword-case"),
                RuleId("standard:line-ending-lf"),
                RuleId("standard:literal-case"),
                RuleId("standard:max-blank-lines"),
                RuleId("standard:max-line-length"),
                RuleId("standard:no-consecutive-semicolons"),
                RuleId("standard:no-distinct-parentheses"),
                RuleId("standard:no-else-null"),
                RuleId("standard:no-leading-blank-lines"),
                RuleId("standard:no-leading-whitespace"),
                RuleId("standard:no-redundant-semicolons"),
                RuleId("standard:no-space-after-dot"),
                RuleId("standard:no-space-after-opening-parenthesis"),
                RuleId("standard:no-space-before-closing-parenthesis"),
                RuleId("standard:no-space-before-comma"),
                RuleId("standard:no-space-before-dot"),
                RuleId("standard:no-space-before-function-parenthesis"),
                RuleId("standard:no-space-before-semicolon"),
                RuleId("standard:no-right-join"),
                RuleId("standard:no-select-distinct-with-group-by"),
                RuleId("standard:no-select-trailing-comma"),
                RuleId("standard:no-tab-indentation"),
                RuleId("standard:no-trailing-blank-lines"),
                RuleId("standard:no-trailing-whitespace"),
                RuleId("standard:prefer-coalesce"),
                RuleId("standard:prefer-count-star"),
                RuleId("standard:require-order-by-with-limit"),
                RuleId("standard:set-operator-line-position"),
                RuleId("standard:space-after-block-comment-start"),
                RuleId("standard:space-after-comma"),
                RuleId("standard:space-after-line-comment-marker"),
                RuleId("standard:space-around-binary-operators"),
                RuleId("standard:space-around-comparison-operators"),
                RuleId("standard:space-before-block-comment-end"),
                RuleId("standard:statement-terminator"),
                RuleId("standard:use-is-null"),
            ),
            ruleIds,
        )
    }
}
