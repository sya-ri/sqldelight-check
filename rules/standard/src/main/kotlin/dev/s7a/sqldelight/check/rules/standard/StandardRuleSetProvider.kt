package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentNotEqualOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.DataTypeCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitUnionOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.FinalNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.FunctionNameCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.KeywordCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.LineEndingLfRule
import dev.s7a.sqldelight.check.rules.standard.rules.LiteralCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoConsecutiveSemicolonsRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceAfterDotRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceAfterOpeningParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeClosingParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeDotRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeFunctionParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeSemicolonRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoRightJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTabIndentationRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCoalesceRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCountStarRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterBlockCommentStartRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterLineCommentMarkerRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundBinaryOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundComparisonOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceBeforeBlockCommentEndRule
import dev.s7a.sqldelight.check.rules.standard.rules.UseIsNullRule
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
            RuleProvider(::ConsistentNotEqualOperatorRule),
            RuleProvider(::DataTypeCaseRule),
            RuleProvider(::ExplicitUnionOperatorRule),
            RuleProvider(::FinalNewlineRule),
            RuleProvider(::FunctionNameCaseRule),
            RuleProvider(::KeywordCaseRule),
            RuleProvider(::LineEndingLfRule),
            RuleProvider(::LiteralCaseRule),
            RuleProvider(::MaxBlankLinesRule),
            RuleProvider(::NoConsecutiveSemicolonsRule),
            RuleProvider(::NoLeadingBlankLinesRule),
            RuleProvider(::NoSpaceAfterDotRule),
            RuleProvider(::NoSpaceAfterOpeningParenthesisRule),
            RuleProvider(::NoSpaceBeforeClosingParenthesisRule),
            RuleProvider(::NoSpaceBeforeCommaRule),
            RuleProvider(::NoSpaceBeforeDotRule),
            RuleProvider(::NoSpaceBeforeFunctionParenthesisRule),
            RuleProvider(::NoSpaceBeforeSemicolonRule),
            RuleProvider(::NoRightJoinRule),
            RuleProvider(::NoTabIndentationRule),
            RuleProvider(::NoTrailingBlankLinesRule),
            RuleProvider(::NoTrailingWhitespaceRule),
            RuleProvider(::PreferCoalesceRule),
            RuleProvider(::PreferCountStarRule),
            RuleProvider(::SpaceAfterBlockCommentStartRule),
            RuleProvider(::SpaceAfterCommaRule),
            RuleProvider(::SpaceAfterLineCommentMarkerRule),
            RuleProvider(::SpaceAroundBinaryOperatorsRule),
            RuleProvider(::SpaceAroundComparisonOperatorsRule),
            RuleProvider(::SpaceBeforeBlockCommentEndRule),
            RuleProvider(::UseIsNullRule),
        )
}
