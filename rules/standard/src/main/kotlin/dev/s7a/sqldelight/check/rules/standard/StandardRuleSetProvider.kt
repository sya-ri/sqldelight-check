package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rules.standard.rules.ClauseKeywordNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentColumnReferencesRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentNotEqualOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentOrderByDirectionRule
import dev.s7a.sqldelight.check.rules.standard.rules.DataTypeCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitCrossJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitInnerJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitUnionOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.FinalNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.FunctionNameCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.KeywordCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.LineEndingLfRule
import dev.s7a.sqldelight.check.rules.standard.rules.LiteralCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxCaseDepthRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxJoinsRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxLineLengthRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxSubqueryDepthRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoConsecutiveSemicolonsRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoDeleteWithoutWhereRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoDistinctParenthesesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoElseNullRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoFromSubqueryRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingWildcardLikeRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoRedundantSemicolonsRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceAfterDotRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceAfterOpeningParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeClosingParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeDotRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeFunctionParenthesisRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpaceBeforeSemicolonRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoRightJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSelectDistinctWithGroupByRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSelectStarRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSelectTrailingCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTabIndentationRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUnnecessaryStatementParenthesesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUpdateWithoutWhereRule
import dev.s7a.sqldelight.check.rules.standard.rules.OperatorLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCoalesceRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCountStarRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferExplicitColumnListInInsertRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferSimpleBooleanCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireOrderByWithLimitRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireResultColumnAliasRule
import dev.s7a.sqldelight.check.rules.standard.rules.SelectModifierLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.SetOperatorLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterBlockCommentStartRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterLineCommentMarkerRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundBinaryOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundComparisonOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceBeforeBlockCommentEndRule
import dev.s7a.sqldelight.check.rules.standard.rules.StatementTerminatorRule
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
            RuleProvider(::ClauseKeywordNewlineRule),
            RuleProvider(::ConsistentColumnReferencesRule),
            RuleProvider(::ConsistentNotEqualOperatorRule),
            RuleProvider(::ConsistentOrderByDirectionRule),
            RuleProvider(::DataTypeCaseRule),
            RuleProvider(::ExplicitCrossJoinRule),
            RuleProvider(::ExplicitInnerJoinRule),
            RuleProvider(::ExplicitUnionOperatorRule),
            RuleProvider(::FinalNewlineRule),
            RuleProvider(::FunctionNameCaseRule),
            RuleProvider(::KeywordCaseRule),
            RuleProvider(::LineEndingLfRule),
            RuleProvider(::LiteralCaseRule),
            RuleProvider(::MaxBlankLinesRule),
            RuleProvider(::MaxCaseDepthRule),
            RuleProvider(::MaxJoinsRule),
            RuleProvider(::MaxLineLengthRule),
            RuleProvider(::MaxSubqueryDepthRule),
            RuleProvider(::NoConsecutiveSemicolonsRule),
            RuleProvider(::NoDeleteWithoutWhereRule),
            RuleProvider(::NoDistinctParenthesesRule),
            RuleProvider(::NoElseNullRule),
            RuleProvider(::NoFromSubqueryRule),
            RuleProvider(::NoLeadingBlankLinesRule),
            RuleProvider(::NoLeadingCommaRule),
            RuleProvider(::NoLeadingWhitespaceRule),
            RuleProvider(::NoLeadingWildcardLikeRule),
            RuleProvider(::NoRedundantSemicolonsRule),
            RuleProvider(::NoSpaceAfterDotRule),
            RuleProvider(::NoSpaceAfterOpeningParenthesisRule),
            RuleProvider(::NoSpaceBeforeClosingParenthesisRule),
            RuleProvider(::NoSpaceBeforeCommaRule),
            RuleProvider(::NoSpaceBeforeDotRule),
            RuleProvider(::NoSpaceBeforeFunctionParenthesisRule),
            RuleProvider(::NoSpaceBeforeSemicolonRule),
            RuleProvider(::NoRightJoinRule),
            RuleProvider(::NoSelectDistinctWithGroupByRule),
            RuleProvider(::NoSelectStarRule),
            RuleProvider(::NoSelectTrailingCommaRule),
            RuleProvider(::NoTabIndentationRule),
            RuleProvider(::NoTrailingBlankLinesRule),
            RuleProvider(::NoTrailingWhitespaceRule),
            RuleProvider(::NoUnnecessaryStatementParenthesesRule),
            RuleProvider(::NoUpdateWithoutWhereRule),
            RuleProvider(::OperatorLinePositionRule),
            RuleProvider(::PreferCoalesceRule),
            RuleProvider(::PreferCountStarRule),
            RuleProvider(::PreferExplicitColumnListInInsertRule),
            RuleProvider(::PreferSimpleBooleanCaseRule),
            RuleProvider(::RequireOrderByWithLimitRule),
            RuleProvider(::RequireResultColumnAliasRule),
            RuleProvider(::SelectModifierLinePositionRule),
            RuleProvider(::SetOperatorLinePositionRule),
            RuleProvider(::SpaceAfterBlockCommentStartRule),
            RuleProvider(::SpaceAfterCommaRule),
            RuleProvider(::SpaceAfterLineCommentMarkerRule),
            RuleProvider(::SpaceAroundBinaryOperatorsRule),
            RuleProvider(::SpaceAroundComparisonOperatorsRule),
            RuleProvider(::SpaceBeforeBlockCommentEndRule),
            RuleProvider(::StatementTerminatorRule),
            RuleProvider(::UseIsNullRule),
        )
}
