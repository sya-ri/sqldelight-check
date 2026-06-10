package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement

/**
 * Utility for merging rule set defaults with explicit rule-level overrides.
 */
public object EnablementResolver {
    /**
     * Resolves final rule enablement before `Auto` applicability is evaluated.
     */
    public fun resolveRuleEnablement(
        ruleSetEnablement: Enablement,
        ruleEnablement: Enablement,
    ): Enablement =
        if (ruleEnablement != Enablement.Auto) {
            ruleEnablement
        } else {
            ruleSetEnablement
        }
}
