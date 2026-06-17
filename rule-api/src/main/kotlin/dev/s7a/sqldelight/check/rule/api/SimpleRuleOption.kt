package dev.s7a.sqldelight.check.rule.api

internal class SimpleRuleOption<T>(
    override val name: String,
    private val defaultValue: T,
    override val deprecation: RuleOptionDeprecation?,
    private val parser: (String) -> T,
) : RuleOption<T> {
    override fun read(values: Map<String, String>): T =
        values[name]?.let(parser) ?: defaultValue

    override fun toString(): String = "RuleOption(name=$name, defaultValue=$defaultValue, deprecation=$deprecation)"
}
