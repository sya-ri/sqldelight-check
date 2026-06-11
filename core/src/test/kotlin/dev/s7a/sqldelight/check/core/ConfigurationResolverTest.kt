package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for configuration precedence.
 */
class ConfigurationResolverTest {
    @Test
    fun `database rule overrides global rule`() {
        val ruleId = qualifiedRuleId("standard:keyword-case")
        val resolver =
            ConfigurationResolver(
                CheckConfig(
                    rules =
                        mapOf(
                            ruleId to RuleConfig(ruleId, Enablement.Enabled, Severity.Error),
                        ),
                    databases =
                        mapOf(
                            "MainDb" to
                                DatabaseConfig(
                                    name = "MainDb",
                                    rules =
                                        mapOf(
                                            ruleId to RuleConfig(ruleId, Enablement.Disabled, Severity.Info),
                                        ),
                                ),
                        ),
                ),
            )

        val resolved = resolver.resolveRule(ruleId, "MainDb")

        assertEquals(Enablement.Disabled, resolved.enablement)
        assertEquals(Severity.Info, resolved.severity)
    }

    @Test
    fun `database rule options override and extend global rule options`() {
        val ruleId = qualifiedRuleId("standard:max-joins")
        val resolver =
            ConfigurationResolver(
                CheckConfig(
                    rules =
                        mapOf(
                            ruleId to
                                RuleConfig(
                                    ruleId,
                                    Enablement.Auto,
                                    Severity.Warning,
                                    options = mapOf("max" to "8", "mode" to "global"),
                                ),
                        ),
                    databases =
                        mapOf(
                            "MainDb" to
                                DatabaseConfig(
                                    name = "MainDb",
                                    rules =
                                        mapOf(
                                            ruleId to
                                                RuleConfig(
                                                    ruleId,
                                                    Enablement.Auto,
                                                    Severity.Warning,
                                                    options = mapOf("max" to "12"),
                                                ),
                                        ),
                                ),
                        ),
                ),
            )

        val resolved = resolver.resolveRule(ruleId, "MainDb")

        assertEquals(mapOf("max" to "12", "mode" to "global"), resolved.options)
    }

    @Test
    fun `database rule set overrides global rule set`() {
        val ruleSetId = RuleSetId("sqlite")
        val resolver =
            ConfigurationResolver(
                CheckConfig(
                    ruleSets =
                        mapOf(
                            ruleSetId to RuleSetConfig(ruleSetId, Enablement.Disabled),
                        ),
                    databases =
                        mapOf(
                            "MainDb" to
                                DatabaseConfig(
                                    name = "MainDb",
                                    ruleSets =
                                        mapOf(
                                            ruleSetId to RuleSetConfig(ruleSetId, Enablement.Enabled),
                                        ),
                                ),
                        ),
                ),
            )

        val resolved = resolver.resolveRuleSet(ruleSetId, "MainDb")

        assertEquals(Enablement.Enabled, resolved.enablement)
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
