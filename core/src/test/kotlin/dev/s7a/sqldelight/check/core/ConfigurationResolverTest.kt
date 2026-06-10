package dev.s7a.sqldelight.check.core

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
        val ruleId = RuleId("standard:keyword-case")
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
    fun `rule enablement overrides disabled rule set`() {
        val result =
            EnablementResolver.resolveRuleEnablement(
                ruleSetEnablement = Enablement.Disabled,
                ruleEnablement = Enablement.Enabled,
            )

        assertEquals(Enablement.Enabled, result)
    }

    @Test
    fun `auto rule inherits rule set enablement`() {
        val result =
            EnablementResolver.resolveRuleEnablement(
                ruleSetEnablement = Enablement.Disabled,
                ruleEnablement = Enablement.Auto,
            )

        assertEquals(Enablement.Disabled, result)
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

