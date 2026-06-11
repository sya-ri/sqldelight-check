package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule within its containing rule set.
 */
@JvmInline
public value class RuleId(
    /**
     * Stable rule-local ID value, such as `no-select-star`.
     */
    public val value: String,
) {
    init {
        require(value.matches(RuleIdPattern)) {
            "Rule ID must use lowercase kebab-case with letters and digits: $value"
        }
    }

    private companion object {
        private val RuleIdPattern = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    }
}
