package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Stable analysis view exposed to custom rules.
 *
 * This type intentionally does not expose SQLDelight compiler internals. Adapter implementations translate
 * SQLDelight-version-specific structures into sqldelight-check-owned models before rules run.
 */
public interface RuleContext {
    /** Database containing the file being analyzed. */
    public val database: DatabaseContext

    /** File being analyzed. */
    public val file: SourceFile
}

/**
 * Receives diagnostics emitted by a rule.
 */
public fun interface DiagnosticReporter {
    /** Records a diagnostic. */
    public fun report(diagnostic: Diagnostic)
}

/**
 * Lint or formatting rule contributed by a rule set.
 */
public interface Rule {
    /** Globally unique rule ID using `rule-set:rule-name` form. */
    public val id: RuleId

    /** Default severity used when user configuration does not override it. */
    public val defaultSeverity: Severity

    /** Default enablement before user configuration and auto applicability are resolved. */
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

/**
 * Creates fresh rule instances for thread-safe analysis.
 */
public fun interface RuleProvider {
    /** Creates a new rule instance. */
    public fun create(): Rule
}

/**
 * Provides a named collection of rules.
 */
public interface RuleSetProvider {
    /** Rule set ID advertised to Gradle DSL and reports. */
    public val id: RuleSetId

    /** Providers for rules in this set. */
    public fun ruleProviders(): Set<RuleProvider>
}

