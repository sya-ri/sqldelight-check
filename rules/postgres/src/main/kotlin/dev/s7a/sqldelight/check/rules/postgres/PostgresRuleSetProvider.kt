package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinementProvider
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.postgres.refinements.PostgresTriggerSourceIndentationRefinement
import dev.s7a.sqldelight.check.rules.postgres.refinements.PostgresTriggerStatementTerminatorRefinement
import dev.s7a.sqldelight.check.rules.postgres.refinements.PostgresTriggerUpdateEventRefinement
import dev.s7a.sqldelight.check.rules.postgres.rules.ExcessiveLocksRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoAddColumnWithVolatileDefaultRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoConcurrentIndexInTransactionRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoDropColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoRenameColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoRenameTableRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoSetNotNullOnExistingColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.PreferIdentityOverSerialRule
import dev.s7a.sqldelight.check.rules.postgres.rules.ReindexConcurrentlyRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RequireConcurrentIndexRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RequireNotValidConstraintRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RiskyAlterTableRule

/**
 * Provides rules for PostgreSQL dialect projects.
 *
 * Individual rules declare the PostgreSQL target capability so `Auto`
 * enablement can be resolved per rule.
 */
public class PostgresRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("postgres")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::ExcessiveLocksRule),
            RuleProvider(::RequireConcurrentIndexRule),
            RuleProvider(::NoConcurrentIndexInTransactionRule),
            RuleProvider(::RequireNotValidConstraintRule),
            RuleProvider(::NoSetNotNullOnExistingColumnRule),
            RuleProvider(::NoAddColumnWithVolatileDefaultRule),
            RuleProvider(::PreferIdentityOverSerialRule),
            RuleProvider(::NoDropColumnRule),
            RuleProvider(::NoRenameColumnRule),
            RuleProvider(::NoRenameTableRule),
            RuleProvider(::ReindexConcurrentlyRule),
            RuleProvider(::RiskyAlterTableRule),
        )

    override fun diagnosticRefinementProviders(): Set<DiagnosticRefinementProvider> =
        setOf(
            DiagnosticRefinementProvider(::PostgresTriggerUpdateEventRefinement),
            DiagnosticRefinementProvider(::PostgresTriggerSourceIndentationRefinement),
            DiagnosticRefinementProvider(::PostgresTriggerStatementTerminatorRefinement),
        )
}
