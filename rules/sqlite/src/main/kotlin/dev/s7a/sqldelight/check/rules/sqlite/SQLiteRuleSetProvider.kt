package dev.s7a.sqldelight.check.rules.sqlite

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
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
 */
public class SQLiteRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("sqlite")

    /**
     * Dialect capability this rule set targets.
     *
     * FIXME: Move this to RuleSetProvider when the public API supports rule-set-level capability gates.
     */
    public val targetCapability: DialectCapability = DialectCapabilities.SQLite

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
