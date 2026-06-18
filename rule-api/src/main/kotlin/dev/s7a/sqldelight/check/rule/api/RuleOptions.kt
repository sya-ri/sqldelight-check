package dev.s7a.sqldelight.check.rule.api

/**
 * Resolved options for the rule currently being executed.
 */
public class RuleOptions(
    private val values: Map<String, String> = emptyMap(),
) {
    /**
     * Reads an option through a typed rule option declaration.
     */
    public operator fun <T> get(option: RuleOption<T>): T = option.read(values)

    /**
     * Reads the raw string value for compatibility with custom rules.
     */
    public operator fun get(name: String): String? = values[name]

    /**
     * Reads the raw string value or fails when the option is missing.
     */
    public fun getValue(name: String): String = values.getValue(name)

    /**
     * Returns whether a raw option value is present.
     */
    public operator fun contains(name: String): Boolean = name in values

    /**
     * Returns the configured option names.
     */
    public val names: Set<String>
        get() = values.keys

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleOptions &&
            values == other.values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "RuleOptions(values=$values)"
}
