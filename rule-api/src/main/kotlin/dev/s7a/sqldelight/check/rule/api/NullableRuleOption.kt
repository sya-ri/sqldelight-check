package dev.s7a.sqldelight.check.rule.api

internal class NullableRuleOption<T>(
    override val name: String,
    override val deprecation: RuleOptionDeprecation?,
    private val parser: (String) -> T,
) : RuleOption<T?> {
    override fun read(values: Map<String, String>): T? =
        values[name]?.let(parser)

    override fun toString(): String = "RuleOption(name=$name, deprecation=$deprecation)"
}
