package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rules.standard.rules.BlankLineBetweenStatementsRule
import dev.s7a.sqldelight.check.rules.standard.rules.BlockedWordsRule
import dev.s7a.sqldelight.check.rules.standard.rules.AvoidModelBoundInsertForPublicApiRule
import dev.s7a.sqldelight.check.rules.standard.rules.ClauseKeywordNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentColumnReferencesRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentNotEqualOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentOrderByDirectionRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentParameterNamesRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentReferenceQualificationRule
import dev.s7a.sqldelight.check.rules.standard.rules.ConsistentSetOperationColumnCountRule
import dev.s7a.sqldelight.check.rules.standard.rules.CteNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.DataTypeCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitCrossJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitInnerJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.ExplicitUnionOperatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.FinalNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.FunctionNameCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.GroupByTargetNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.JoinNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.KeywordCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.LineEndingLfRule
import dev.s7a.sqldelight.check.rules.standard.rules.LiteralCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.GroupedStatementHasSinglePurposeRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxCaseDepthRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxJoinsRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxLineLengthRule
import dev.s7a.sqldelight.check.rules.standard.rules.MaxSubqueryDepthRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoConsecutiveSemicolonsRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoBlankLineAfterQueryLabelRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoDeleteWithoutWhereRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoDistinctParenthesesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoElseNullRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoFromSubqueryRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoImplicitCrossJoinCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoLeadingWildcardLikeRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoNotInNullableSubqueryRule
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
import dev.s7a.sqldelight.check.rules.standard.rules.NoSelfColumnAliasRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSelfAliasRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoSpecialCharacterIdentifiersRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTabIndentationRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingBlankLinesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingWhitespaceRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTransactionInMigrationRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUnknownQualifierRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUnusedCteRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUnusedJoinRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUnnecessaryStatementParenthesesRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoUpdateWithoutWhereRule
import dev.s7a.sqldelight.check.rules.standard.rules.OperatorLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.OrderByTargetNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCoalesceRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferBetweenForInclusiveRangeRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferCountStarRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferExistsOverCountForExistenceRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferExplicitColumnListInInsertRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferNamedParametersRule
import dev.s7a.sqldelight.check.rules.standard.rules.PreferSimpleBooleanCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.QueryNameCaseRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireExplicitNullOrderingRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireOrderByWithLimitRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireColumnAliasAsRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireParenthesesForMixedBooleanOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireQueryLabelRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireResultColumnAliasRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireTableAliasAsRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireTableAliasForSubqueryRule
import dev.s7a.sqldelight.check.rules.standard.rules.RequireWhereIndexFriendlyPredicateRule
import dev.s7a.sqldelight.check.rules.standard.rules.SelectCommaLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.SelectModifierLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.SelectTargetNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.SetOperatorLinePositionRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterBlockCommentStartRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterCommaRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAfterLineCommentMarkerRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundBinaryOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceAroundComparisonOperatorsRule
import dev.s7a.sqldelight.check.rules.standard.rules.SpaceBeforeBlockCommentEndRule
import dev.s7a.sqldelight.check.rules.standard.rules.StatementTerminatorRule
import dev.s7a.sqldelight.check.rules.standard.rules.UniqueTableAliasesRule
import dev.s7a.sqldelight.check.rules.standard.rules.UniqueColumnAliasesRule
import dev.s7a.sqldelight.check.rules.standard.rules.UseIsNullRule
import dev.s7a.sqldelight.check.rules.standard.rules.WhereConditionNewlineRule
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
            RuleProvider(::BlankLineBetweenStatementsRule),
            RuleProvider(::AvoidModelBoundInsertForPublicApiRule),
            RuleProvider(::BlockedWordsRule),
            RuleProvider(::ClauseKeywordNewlineRule),
            RuleProvider(::ConsistentColumnReferencesRule),
            RuleProvider(::ConsistentNotEqualOperatorRule),
            RuleProvider(::ConsistentOrderByDirectionRule),
            RuleProvider(::ConsistentParameterNamesRule),
            RuleProvider(::ConsistentReferenceQualificationRule),
            RuleProvider(::ConsistentSetOperationColumnCountRule),
            RuleProvider(::CteNewlineRule),
            RuleProvider(::DataTypeCaseRule),
            RuleProvider(::ExplicitCrossJoinRule),
            RuleProvider(::ExplicitInnerJoinRule),
            RuleProvider(::ExplicitUnionOperatorRule),
            RuleProvider(::FinalNewlineRule),
            RuleProvider(::FunctionNameCaseRule),
            RuleProvider(::GroupByTargetNewlineRule),
            RuleProvider(::GroupedStatementHasSinglePurposeRule),
            RuleProvider(::JoinNewlineRule),
            RuleProvider(::KeywordCaseRule),
            RuleProvider(::LineEndingLfRule),
            RuleProvider(::LiteralCaseRule),
            RuleProvider(::MaxBlankLinesRule),
            RuleProvider(::MaxCaseDepthRule),
            RuleProvider(::MaxJoinsRule),
            RuleProvider(::MaxLineLengthRule),
            RuleProvider(::MaxSubqueryDepthRule),
            RuleProvider(::NoConsecutiveSemicolonsRule),
            RuleProvider(::NoBlankLineAfterQueryLabelRule),
            RuleProvider(::NoDeleteWithoutWhereRule),
            RuleProvider(::NoDistinctParenthesesRule),
            RuleProvider(::NoElseNullRule),
            RuleProvider(::NoFromSubqueryRule),
            RuleProvider(::NoImplicitCrossJoinCommaRule),
            RuleProvider(::NoLeadingBlankLinesRule),
            RuleProvider(::NoLeadingCommaRule),
            RuleProvider(::NoLeadingWhitespaceRule),
            RuleProvider(::NoLeadingWildcardLikeRule),
            RuleProvider(::NoNotInNullableSubqueryRule),
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
            RuleProvider(::NoSelfColumnAliasRule),
            RuleProvider(::NoSelfAliasRule),
            RuleProvider(::NoSpecialCharacterIdentifiersRule),
            RuleProvider(::NoTabIndentationRule),
            RuleProvider(::NoTrailingBlankLinesRule),
            RuleProvider(::NoTrailingWhitespaceRule),
            RuleProvider(::NoTransactionInMigrationRule),
            RuleProvider(::NoUnknownQualifierRule),
            RuleProvider(::NoUnusedCteRule),
            RuleProvider(::NoUnusedJoinRule),
            RuleProvider(::NoUnnecessaryStatementParenthesesRule),
            RuleProvider(::NoUpdateWithoutWhereRule),
            RuleProvider(::OperatorLinePositionRule),
            RuleProvider(::OrderByTargetNewlineRule),
            RuleProvider(::PreferBetweenForInclusiveRangeRule),
            RuleProvider(::PreferCoalesceRule),
            RuleProvider(::PreferCountStarRule),
            RuleProvider(::PreferExistsOverCountForExistenceRule),
            RuleProvider(::PreferExplicitColumnListInInsertRule),
            RuleProvider(::PreferNamedParametersRule),
            RuleProvider(::PreferSimpleBooleanCaseRule),
            RuleProvider(::QueryNameCaseRule),
            RuleProvider(::RequireExplicitNullOrderingRule),
            RuleProvider(::RequireOrderByWithLimitRule),
            RuleProvider(::RequireColumnAliasAsRule),
            RuleProvider(::RequireParenthesesForMixedBooleanOperatorsRule),
            RuleProvider(::RequireQueryLabelRule),
            RuleProvider(::RequireResultColumnAliasRule),
            RuleProvider(::RequireTableAliasAsRule),
            RuleProvider(::RequireTableAliasForSubqueryRule),
            RuleProvider(::RequireWhereIndexFriendlyPredicateRule),
            RuleProvider(::SelectCommaLinePositionRule),
            RuleProvider(::SelectModifierLinePositionRule),
            RuleProvider(::SelectTargetNewlineRule),
            RuleProvider(::SetOperatorLinePositionRule),
            RuleProvider(::SpaceAfterBlockCommentStartRule),
            RuleProvider(::SpaceAfterCommaRule),
            RuleProvider(::SpaceAfterLineCommentMarkerRule),
            RuleProvider(::SpaceAroundBinaryOperatorsRule),
            RuleProvider(::SpaceAroundComparisonOperatorsRule),
            RuleProvider(::SpaceBeforeBlockCommentEndRule),
            RuleProvider(::StatementTerminatorRule),
            RuleProvider(::UniqueColumnAliasesRule),
            RuleProvider(::UniqueTableAliasesRule),
            RuleProvider(::UseIsNullRule),
            RuleProvider(::WhereConditionNewlineRule),
        )
}
