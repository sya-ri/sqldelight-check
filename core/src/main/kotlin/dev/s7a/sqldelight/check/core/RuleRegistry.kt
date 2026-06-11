package dev.s7a.sqldelight.check.core

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
                provider.id.value to ruleProvider.create().id
            }
        }

    val invalidRuleIds =
        ruleIds
            .filter { (_, ruleId) -> ':' in ruleId }
            .map { (ruleSetId, ruleId) -> "$ruleSetId:$ruleId" }
            .sorted()
    require(invalidRuleIds.isEmpty()) {
        "Rule IDs must be local and must not contain ':': ${invalidRuleIds.joinToString()}"
    }

    val duplicateRuleIds =
        ruleIds
            .map { (ruleSetId, ruleId) -> ruleSetId to "$ruleSetId:$ruleId" }
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
