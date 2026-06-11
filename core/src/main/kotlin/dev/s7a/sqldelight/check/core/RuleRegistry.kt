package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader

/**
 * Registry of rule set providers discovered from the runtime classpath.
 */
public class RuleRegistry private constructor(
    private val ruleSetProviders: List<RuleSetProvider>,
) {
    init {
        validateRuleSetProviders(ruleSetProviders)
    }

    /**
     * Returns discovered rule set providers.
     */
    public fun providers(): List<RuleSetProvider> = ruleSetProviders

    public companion object {
        internal fun create(providers: List<RuleSetProvider>): RuleRegistry = RuleRegistry(providers)

        /**
         * Loads rule set providers visible to [classLoader].
         */
        public fun load(classLoader: ClassLoader): RuleRegistry {
            val providers =
                ServiceLoader
                    .load(RuleSetProvider::class.java, classLoader)
                    .toList()
            return RuleRegistry(providers)
        }
    }
}

private fun validateRuleSetProviders(providers: List<RuleSetProvider>) {
    val duplicateRuleSetIds =
        providers
            .groupBy { provider -> provider.id.value }
            .filterValues { matches -> matches.size > 1 }
            .keys
            .sorted()
    require(duplicateRuleSetIds.isEmpty()) {
        "Duplicate sqldelight-check rule set provider ID(s): ${duplicateRuleSetIds.joinToString()}"
    }

    val ruleIds =
        providers.flatMap { provider ->
            provider.ruleProviders().map { ruleProvider ->
                provider.id to ruleProvider.create().id
            }
        }

    val duplicateRuleIds =
        ruleIds
            .map { (ruleSetId, ruleId) -> ruleSetId.value to QualifiedRuleId(ruleSetId, ruleId).value }
            .groupBy { (_, ruleId) -> ruleId }
            .filterValues { matches -> matches.size > 1 }
            .mapValues { (_, matches) ->
                matches.map { (ruleSetId, _) -> ruleSetId }.distinct().sorted()
            }
            .toSortedMap()
    require(duplicateRuleIds.isEmpty()) {
        duplicateRuleIds
            .entries
            .joinToString(
                prefix = "Duplicate sqldelight-check rule ID(s): ",
            ) { (ruleId, ruleSetIds) ->
                "$ruleId in ${ruleSetIds.joinToString()}"
            }
    }
}
