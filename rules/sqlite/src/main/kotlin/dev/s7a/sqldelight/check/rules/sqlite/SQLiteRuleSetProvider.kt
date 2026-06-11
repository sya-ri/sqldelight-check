package dev.s7a.sqldelight.check.rules.sqlite

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.sqlite.rules.ConsistentConflictResolutionRule
import dev.s7a.sqldelight.check.rules.sqlite.rules.ForeignKeysRestoredRule
import dev.s7a.sqldelight.check.rules.sqlite.rules.NoAlterTableComplexChangeRule
import dev.s7a.sqldelight.check.rules.sqlite.rules.NoAutoincrementWithoutNeedRule
import dev.s7a.sqldelight.check.rules.sqlite.rules.PreferIntegerPrimaryKeyRule
import dev.s7a.sqldelight.check.rules.sqlite.rules.PreferWithoutRowidForCompositePkRule

/**
 * Provides rules for SQLite dialect projects.
 *
 * Individual rules declare the SQLite target capability so `Auto` enablement
 * can be resolved per rule.
 */
public class SQLiteRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("sqlite")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::ConsistentConflictResolutionRule),
            RuleProvider(::ForeignKeysRestoredRule),
            RuleProvider(::PreferIntegerPrimaryKeyRule),
            RuleProvider(::NoAutoincrementWithoutNeedRule),
            RuleProvider(::NoAlterTableComplexChangeRule),
            RuleProvider(::PreferWithoutRowidForCompositePkRule),
        )
}
