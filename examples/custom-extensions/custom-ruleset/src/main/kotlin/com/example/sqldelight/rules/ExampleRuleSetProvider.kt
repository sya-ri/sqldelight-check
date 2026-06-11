package com.example.sqldelight.rules

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

public class ExampleRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("example")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NoSelectStarRule),
        )
}
