package dev.s7a.sqldelight.check.rules.standard.rules

internal fun Map<String, String>.positiveIntOption(
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

internal fun Map<String, String>.booleanOption(
    name: String,
    defaultValue: Boolean,
): Boolean {
    val value = this[name] ?: return defaultValue
    return requireNotNull(value.toBooleanStrictOrNull()) {
        "Option '$name' must be true or false."
    }
}
