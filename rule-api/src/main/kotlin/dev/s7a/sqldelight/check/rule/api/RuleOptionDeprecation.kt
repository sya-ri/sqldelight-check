package dev.s7a.sqldelight.check.rule.api

/**
 * Deprecation metadata for a rule option that remains available for explicit users.
 */
public class RuleOptionDeprecation(
    /**
     * Human-readable migration note for this deprecated option.
     */
    public val message: String,
    /**
     * Replacement option name, when a direct replacement exists.
     */
    public val replacement: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleOptionDeprecation &&
            message == other.message &&
            replacement == other.replacement

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + (replacement?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "RuleOptionDeprecation(message=$message, replacement=$replacement)"
}
