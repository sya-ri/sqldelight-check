package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader

/**
 * Registry of rule set providers discovered from the runtime classpath.
 */
public class RuleRegistry private constructor(
    private val ruleSetProviders: List<RuleSetProvider>,
) {
    /**
     * Returns discovered rule set providers.
     */
    public fun providers(): List<RuleSetProvider> = ruleSetProviders

    public companion object {
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
