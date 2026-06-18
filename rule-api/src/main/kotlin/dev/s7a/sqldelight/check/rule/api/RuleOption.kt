package dev.s7a.sqldelight.check.rule.api

/**
 * Typed declaration for one rule option.
 */
public interface RuleOption<T> {
    /**
     * Option name in configuration.
     */
    public val name: String

    /**
     * Deprecation metadata when this option remains available for compatibility.
     */
    public val deprecation: RuleOptionDeprecation?
        get() = null

    /**
     * Reads this option from raw resolved options.
     */
    public fun read(values: Map<String, String>): T
}
