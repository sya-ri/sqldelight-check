package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.QualifiedRuleId



import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
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
                .map { provider -> QualifiedRuleId(StandardRuleSetProvider().id, provider.create().id) }
                .toSet()

        assertEquals(
            setOf(
                qualifiedRuleId("standard:blank-line-between-statements"),
                qualifiedRuleId("standard:avoid-model-bound-insert-for-public-api"),
                qualifiedRuleId("standard:blocked-words"),
                qualifiedRuleId("standard:case-branch-newline"),
                qualifiedRuleId("standard:clause-keyword-newline"),
                qualifiedRuleId("standard:consistent-column-references"),
                qualifiedRuleId("standard:consistent-not-equal-operator"),
                qualifiedRuleId("standard:consistent-order-by-direction"),
                qualifiedRuleId("standard:consistent-parameter-names"),
                qualifiedRuleId("standard:consistent-reference-qualification"),
                qualifiedRuleId("standard:consistent-set-operation-column-count"),
                qualifiedRuleId("standard:constraint-newline"),
                qualifiedRuleId("standard:cte-newline"),
                qualifiedRuleId("standard:data-type-case"),
                qualifiedRuleId("standard:explicit-cross-join"),
                qualifiedRuleId("standard:explicit-inner-join"),
                qualifiedRuleId("standard:explicit-union-operator"),
                qualifiedRuleId("standard:final-newline"),
                qualifiedRuleId("standard:function-name-case"),
                qualifiedRuleId("standard:group-by-target-newline"),
                qualifiedRuleId("standard:group-statement-count-limit"),
                qualifiedRuleId("standard:grouped-statement-has-single-purpose"),
                qualifiedRuleId("standard:import-order"),
                qualifiedRuleId("standard:insert-values-newline"),
                qualifiedRuleId("standard:join-newline"),
                qualifiedRuleId("standard:keyword-case"),
                qualifiedRuleId("standard:line-ending-lf"),
                qualifiedRuleId("standard:literal-case"),
                qualifiedRuleId("standard:mapped-type-name-case"),
                qualifiedRuleId("standard:max-blank-lines"),
                qualifiedRuleId("standard:max-case-depth"),
                qualifiedRuleId("standard:max-joins"),
                qualifiedRuleId("standard:max-line-length"),
                qualifiedRuleId("standard:max-subquery-depth"),
                qualifiedRuleId("standard:no-blank-line-after-query-label"),
                qualifiedRuleId("standard:no-consecutive-semicolons"),
                qualifiedRuleId("standard:no-delete-without-where"),
                qualifiedRuleId("standard:no-distinct-parentheses"),
                qualifiedRuleId("standard:no-drop-table-in-migration"),
                qualifiedRuleId("standard:no-duplicate-import"),
                qualifiedRuleId("standard:no-duplicate-query-label"),
                qualifiedRuleId("standard:no-else-null"),
                qualifiedRuleId("standard:no-from-subquery"),
                qualifiedRuleId("standard:no-implicit-cross-join-comma"),
                qualifiedRuleId("standard:no-leading-blank-lines"),
                qualifiedRuleId("standard:no-leading-comma"),
                qualifiedRuleId("standard:no-leading-whitespace"),
                qualifiedRuleId("standard:no-leading-wildcard-like"),
                qualifiedRuleId("standard:no-not-in-nullable-subquery"),
                qualifiedRuleId("standard:no-offset-pagination"),
                qualifiedRuleId("standard:no-order-by-ordinal"),
                qualifiedRuleId("standard:no-redundant-semicolons"),
                qualifiedRuleId("standard:no-space-after-dot"),
                qualifiedRuleId("standard:no-space-after-opening-parenthesis"),
                qualifiedRuleId("standard:no-space-before-closing-parenthesis"),
                qualifiedRuleId("standard:no-space-before-comma"),
                qualifiedRuleId("standard:no-space-before-dot"),
                qualifiedRuleId("standard:no-space-before-function-parenthesis"),
                qualifiedRuleId("standard:no-space-before-semicolon"),
                qualifiedRuleId("standard:no-right-join"),
                qualifiedRuleId("standard:no-select-distinct-with-group-by"),
                qualifiedRuleId("standard:no-select-star"),
                qualifiedRuleId("standard:no-select-trailing-comma"),
                qualifiedRuleId("standard:no-select-star-in-view"),
                qualifiedRuleId("standard:no-self-column-alias"),
                qualifiedRuleId("standard:no-self-alias"),
                qualifiedRuleId("standard:no-special-character-identifiers"),
                qualifiedRuleId("standard:no-tab-indentation"),
                qualifiedRuleId("standard:no-trailing-blank-lines"),
                qualifiedRuleId("standard:no-trailing-whitespace"),
                qualifiedRuleId("standard:no-transaction-in-migration"),
                qualifiedRuleId("standard:no-unknown-qualifier"),
                qualifiedRuleId("standard:no-unused-cte"),
                qualifiedRuleId("standard:no-unused-join"),
                qualifiedRuleId("standard:no-unnecessary-statement-parentheses"),
                qualifiedRuleId("standard:no-update-without-where"),
                qualifiedRuleId("standard:no-wildcard-import"),
                qualifiedRuleId("standard:operator-line-position"),
                qualifiedRuleId("standard:order-by-target-newline"),
                qualifiedRuleId("standard:parameter-name-case"),
                qualifiedRuleId("standard:parameter-name-matches-column"),
                qualifiedRuleId("standard:prefer-between-for-inclusive-range"),
                qualifiedRuleId("standard:prefer-coalesce"),
                qualifiedRuleId("standard:prefer-count-star"),
                qualifiedRuleId("standard:prefer-exists-over-count-for-existence"),
                qualifiedRuleId("standard:prefer-explicit-column-list-in-insert"),
                qualifiedRuleId("standard:prefer-named-parameters"),
                qualifiedRuleId("standard:prefer-simple-boolean-case"),
                qualifiedRuleId("standard:query-name-case"),
                qualifiedRuleId("standard:query-label-matches-operation"),
                qualifiedRuleId("standard:require-explicit-null-ordering"),
                qualifiedRuleId("standard:require-order-by-with-limit"),
                qualifiedRuleId("standard:require-column-alias-as"),
                qualifiedRuleId("standard:require-alias-for-duplicate-result-names"),
                qualifiedRuleId("standard:require-like-escape-for-user-input"),
                qualifiedRuleId("standard:require-parentheses-for-mixed-boolean-operators"),
                qualifiedRuleId("standard:require-query-label"),
                qualifiedRuleId("standard:require-result-column-alias"),
                qualifiedRuleId("standard:require-table-alias-as"),
                qualifiedRuleId("standard:require-table-alias-for-subquery"),
                qualifiedRuleId("standard:require-where-index-friendly-predicate"),
                qualifiedRuleId("standard:result-alias-name-case"),
                qualifiedRuleId("standard:select-comma-line-position"),
                qualifiedRuleId("standard:select-modifier-line-position"),
                qualifiedRuleId("standard:select-target-newline"),
                qualifiedRuleId("standard:set-operator-line-position"),
                qualifiedRuleId("standard:space-after-block-comment-start"),
                qualifiedRuleId("standard:space-after-comma"),
                qualifiedRuleId("standard:space-after-line-comment-marker"),
                qualifiedRuleId("standard:space-around-binary-operators"),
                qualifiedRuleId("standard:space-around-comparison-operators"),
                qualifiedRuleId("standard:space-before-block-comment-end"),
                qualifiedRuleId("standard:source-indentation"),
                qualifiedRuleId("standard:statement-terminator"),
                qualifiedRuleId("standard:unique-column-aliases"),
                qualifiedRuleId("standard:unique-table-aliases"),
                qualifiedRuleId("standard:use-is-null"),
                qualifiedRuleId("standard:view-name-case"),
                qualifiedRuleId("standard:where-condition-newline"),
            ),
            ruleIds,
        )
    }
}

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
