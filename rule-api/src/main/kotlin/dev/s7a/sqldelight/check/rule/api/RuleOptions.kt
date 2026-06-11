package dev.s7a.sqldelight.check.rule.api

/**
 * Reads a strict boolean option from rule configuration.
 */
public fun Map<String, String>.booleanOption(
    name: String,
    defaultValue: Boolean,
): Boolean {
    val value = this[name] ?: return defaultValue
    return requireNotNull(value.toBooleanStrictOrNull()) {
        "Option '$name' must be true or false."
    }
}

/**
 * Reads a comma-separated string list from rule configuration.
 */
public fun Map<String, String>.commaSeparatedOption(name: String): List<String> =
    get(name)
        ?.split(',')
        ?.map { value -> value.trim() }
        ?.filter { value -> value.isNotEmpty() }
        ?: emptyList()

/**
 * Reads a positive integer option from rule configuration.
 */
public fun Map<String, String>.positiveIntOption(
    name: String,
    defaultValue: Int,
): Int {
    val value = this[name] ?: return defaultValue
    val parsed = value.toIntOrNull()
    require(parsed != null && parsed > 0) {
        "Option '$name' must be a positive integer."
    }
    return parsed
}
