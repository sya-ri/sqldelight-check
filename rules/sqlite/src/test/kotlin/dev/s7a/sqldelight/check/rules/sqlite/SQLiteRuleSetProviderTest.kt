package dev.s7a.sqldelight.check.rules.sqlite

import dev.s7a.sqldelight.check.api.QualifiedRuleId



import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.sqlite.SQLiteDialectCapability
import dev.s7a.sqldelight.check.dialects.sqlite.SQLiteDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the SQLite rule set.
 */
class SQLiteRuleSetProviderTest {
    @Test
    fun `sqlite rule set provides sqlite rules`() {
        val provider = SQLiteRuleSetProvider()
        val rules = provider.ruleProviders().map { ruleProvider -> ruleProvider.create() }
        val ruleIds = rules.map { rule -> QualifiedRuleId(provider.id, rule.id) }.toSet()

        assertEquals(RuleSetId("sqlite"), provider.id)
        assertEquals(setOf(SQLiteDialectCapability), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                qualifiedRuleId("sqlite:consistent-conflict-resolution"),
                qualifiedRuleId("sqlite:foreign-keys-restored"),
                qualifiedRuleId("sqlite:prefer-integer-primary-key"),
                qualifiedRuleId("sqlite:no-autoincrement-without-need"),
                qualifiedRuleId("sqlite:no-alter-table-complex-change"),
                qualifiedRuleId("sqlite:prefer-without-rowid-for-composite-pk"),
            ),
            ruleIds,
        )
    }

    @Test
    fun `sqlite rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("sqlite") in providers)
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
