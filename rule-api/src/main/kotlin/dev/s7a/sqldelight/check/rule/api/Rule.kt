package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Lint or formatting rule contributed by a rule set.
 *
 * Rule implementations are loaded through a [RuleSetProvider] and run after
 * SQLDelight accepts the source file.
 */
public interface Rule {
    /**
     * Rule-local ID, such as `no-select-star`.
     *
     * sqldelight-check combines the containing [RuleSetProvider.id] and this
     * value into the user-facing `rule-set:rule-name` ID used by Gradle
     * configuration, reporters, and source-level disable directives.
     */
    public val id: RuleId

    /**
     * Default severity used when user configuration does not override it.
     *
     * The engine replaces diagnostic severities with the resolved configured
     * severity before reports are written.
     */
    public val defaultSeverity: Severity

    /**
     * Whether this rule can run by default before user configuration and auto
     * applicability are resolved.
     *
     * `true` means the rule uses automatic applicability, so dialect capability
     * and [isApplicable] still decide whether it runs for a file. `false` means
     * the rule is disabled unless user configuration explicitly enables it.
     */
    public val defaultEnable: Boolean
        get() = true

    /**
     * Dialect capability required when this rule is resolved with `Auto`.
     *
     * Rules without a dialect-specific requirement should keep this value null.
     */
    public val targetCapability: DialectCapability?
        get() = null

    /**
     * Returns whether this rule applies to the current context when enablement is `Auto`.
     *
     * Explicit `Enabled` and `Disabled` configuration bypass this method.
     */
    public fun isApplicable(context: RuleContext): Boolean = true

    /**
     * Runs this rule against the current context.
     *
     * Implementations report diagnostics through [reporter] and should not
     * mutate the source file directly.
     */
    public fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    )
}
