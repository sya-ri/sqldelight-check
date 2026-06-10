package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Lint or formatting rule contributed by a rule set.
 */
public interface Rule {
    /**
     * Globally unique rule ID using `rule-set:rule-name` form.
     */
    public val id: RuleId

    /**
     * Default severity used when user configuration does not override it.
     */
    public val defaultSeverity: Severity

    /**
     * Default enablement before user configuration and auto applicability are resolved.
     */
    public val defaultEnablement: Enablement

    /**
     * Returns whether this rule applies to the current context when enablement is `Auto`.
     */
    public fun isApplicable(context: RuleContext): Boolean = true

    /**
     * Runs this rule against the current context.
     */
    public fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    )
}
