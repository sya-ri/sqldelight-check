package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule set such as `standard` or a third-party provider ID.
 */
@JvmInline
public value class RuleSetId(
    /**
     * Stable rule set ID value.
     */
    public val value: String,
) {
    init {
        require(value.matches(RuleSetIdPattern)) {
            "Rule set ID must use lowercase kebab-case with letters and digits: $value"
        }
    }

    private companion object {
        private val RuleSetIdPattern = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    }
}
