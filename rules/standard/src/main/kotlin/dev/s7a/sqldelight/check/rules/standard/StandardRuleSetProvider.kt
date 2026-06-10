package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rules.standard.rules.FinalNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.KeywordCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.LineEndingLfRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoConsecutiveSemicolonsRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceAfterOpeningParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeClosingParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeSemicolonRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTabIndentationRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundComparisonOperatorsRule
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

/**
 * Built-in common rule set for dialect-independent SQL and SQLDelight rules.
 */
public class StandardRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("standard")

    /**
     * Returns rule providers in the standard rule set.
     *
     */
    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::FinalNewlineRule),
            RuleProvider(::LineEndingLfRule),
            RuleProvider(::MaxBlankLinesRule),
            RuleProvider(::NoConsecutiveSemicolonsRule),
            RuleProvider(::NoLeadingBlankLinesRule),
            RuleProvider(::NoSpaceAfterOpeningParenthesisRule),
            RuleProvider(::NoSpaceBeforeClosingParenthesisRule),
            RuleProvider(::NoSpaceBeforeCommaRule),
            RuleProvider(::NoSpaceBeforeSemicolonRule),
            RuleProvider(::NoTabIndentationRule),
            RuleProvider(::NoTrailingBlankLinesRule),
            RuleProvider(::NoTrailingWhitespaceRule),
            RuleProvider(::KeywordCaseRule),
            RuleProvider(::SpaceAfterCommaRule),
            RuleProvider(::SpaceAroundComparisonOperatorsRule),
        )
}
