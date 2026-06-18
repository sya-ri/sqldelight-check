@file:JvmName("RuleOptionsKt")
@file:JvmMultifileClass
@file:Suppress("DuplicatedCode")

package dev.s7a.sqldelight.check.rule.api

import kotlin.enums.enumEntries
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Declares a custom typed rule option and exposes it as a property delegate.
 */
public fun <T> option(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
    parser: (String) -> T,
): ReadOnlyProperty<Rule, RuleOption<T?>> =
    RuleOptionDelegate(NullableRuleOption(name, deprecation, parser))

/**
 * Declares a custom typed rule option with a default value and exposes it as a property delegate.
 */
public fun <T> option(
    name: String,
    defaultValue: T,
    deprecation: RuleOptionDeprecation? = null,
    parser: (String) -> T,
): ReadOnlyProperty<Rule, RuleOption<T>> =
    RuleOptionDelegate(SimpleRuleOption(name, defaultValue, deprecation, parser))

/**
 * Declares a raw string rule option and exposes it as a property delegate.
 */
public fun stringOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<String?>> =
    option(name, deprecation, ::identity)

/**
 * Declares a raw string rule option with a default value and exposes it as a property delegate.
 */
public fun stringOption(
    name: String,
    defaultValue: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<String>> =
    option(name, defaultValue, deprecation, ::identity)

/**
 * Declares a non-blank string rule option and exposes it as a property delegate.
 */
public fun nonBlankStringOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<String?>> =
    option(name, deprecation, nonBlankStringParser(name))

/**
 * Declares a non-blank string rule option with a default value and exposes it as a property delegate.
 */
public fun nonBlankStringOption(
    name: String,
    defaultValue: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<String>> =
    option(name, defaultValue, deprecation, nonBlankStringParser(name))

/**
 * Declares a strict boolean rule option and exposes it as a property delegate.
 */
public fun booleanOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Boolean?>> =
    option(name, deprecation, booleanParser(name))

/**
 * Declares a strict boolean rule option with a default value and exposes it as a property delegate.
 */
public fun booleanOption(
    name: String,
    defaultValue: Boolean,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Boolean>> =
    option(name, defaultValue, deprecation, booleanParser(name))

/**
 * Declares an integer rule option and exposes it as a property delegate.
 */
public fun intOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int?>> =
    option(name, deprecation, intParser(name))

/**
 * Declares an integer rule option with a default value and exposes it as a property delegate.
 */
public fun intOption(
    name: String,
    defaultValue: Int,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int>> =
    option(name, defaultValue, deprecation, intParser(name))

/**
 * Declares a long integer rule option and exposes it as a property delegate.
 */
public fun longOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Long?>> =
    option(name, deprecation, longParser(name))

/**
 * Declares a long integer rule option with a default value and exposes it as a property delegate.
 */
public fun longOption(
    name: String,
    defaultValue: Long,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Long>> =
    option(name, defaultValue, deprecation, longParser(name))

/**
 * Declares a positive integer rule option and exposes it as a property delegate.
 */
public fun positiveIntOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int?>> =
    option(name, deprecation, positiveIntParser(name))

/**
 * Declares a positive integer rule option with a default value and exposes it as a property delegate.
 */
public fun positiveIntOption(
    name: String,
    defaultValue: Int,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int>> =
    option(name, defaultValue, deprecation, positiveIntParser(name))

/**
 * Declares a non-negative integer rule option and exposes it as a property delegate.
 */
public fun nonNegativeIntOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int?>> =
    option(name, deprecation, nonNegativeIntParser(name))

/**
 * Declares a non-negative integer rule option with a default value and exposes it as a property delegate.
 */
public fun nonNegativeIntOption(
    name: String,
    defaultValue: Int,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<Int>> =
    option(name, defaultValue, deprecation, nonNegativeIntParser(name))

/**
 * Declares a comma-separated list rule option and exposes it as a property delegate.
 */
public fun <T> listOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
    itemParser: (String) -> T,
): ReadOnlyProperty<Rule, RuleOption<List<T>?>> =
    option(name, deprecation, commaSeparatedListParser(itemParser))

/**
 * Declares a comma-separated list rule option with a default value and exposes it as a property delegate.
 */
public fun <T> listOption(
    name: String,
    defaultValue: List<T>,
    deprecation: RuleOptionDeprecation? = null,
    itemParser: (String) -> T,
): ReadOnlyProperty<Rule, RuleOption<List<T>>> =
    option(name, defaultValue, deprecation, commaSeparatedListParser(itemParser))

/**
 * Declares a comma-separated string list rule option and exposes it as a property delegate.
 */
public fun stringListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<String>?>> =
    listOption(name, deprecation, ::identity)

/**
 * Declares a comma-separated string list rule option with a default value and exposes it as a property delegate.
 */
public fun stringListOption(
    name: String,
    defaultValue: List<String>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<String>>> =
    listOption(name, defaultValue, deprecation, ::identity)

/**
 * Declares a comma-separated integer list rule option and exposes it as a property delegate.
 */
public fun intListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>?>> =
    listOption(name, deprecation, listItemParser(name, "integer", String::toIntOrNull))

/**
 * Declares a comma-separated integer list rule option with a default value and exposes it as a property delegate.
 */
public fun intListOption(
    name: String,
    defaultValue: List<Int>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>>> =
    listOption(name, defaultValue, deprecation, listItemParser(name, "integer", String::toIntOrNull))

/**
 * Declares a comma-separated positive integer list rule option and exposes it as a property delegate.
 */
public fun positiveIntListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>?>> =
    listOption(name, deprecation, positiveIntListItemParser(name))

/**
 * Declares a comma-separated positive integer list rule option with a default value and exposes it as a property delegate.
 */
public fun positiveIntListOption(
    name: String,
    defaultValue: List<Int>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>>> =
    listOption(name, defaultValue, deprecation, positiveIntListItemParser(name))

/**
 * Declares a comma-separated non-negative integer list rule option and exposes it as a property delegate.
 */
public fun nonNegativeIntListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>?>> =
    listOption(name, deprecation, nonNegativeIntListItemParser(name))

/**
 * Declares a comma-separated non-negative integer list rule option with a default value and exposes it as a property delegate.
 */
public fun nonNegativeIntListOption(
    name: String,
    defaultValue: List<Int>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Int>>> =
    listOption(name, defaultValue, deprecation, nonNegativeIntListItemParser(name))

/**
 * Declares a comma-separated long integer list rule option and exposes it as a property delegate.
 */
public fun longListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Long>?>> =
    listOption(name, deprecation, listItemParser(name, "long integer", String::toLongOrNull))

/**
 * Declares a comma-separated long integer list rule option with a default value and exposes it as a property delegate.
 */
public fun longListOption(
    name: String,
    defaultValue: List<Long>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<Long>>> =
    listOption(name, defaultValue, deprecation, listItemParser(name, "long integer", String::toLongOrNull))

/**
 * Declares an enum rule option and exposes it as a property delegate.
 */
public inline fun <reified T : Enum<T>> enumOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<T?>> =
    option(name, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Option '$name' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.name }}.")
    }

/**
 * Declares an enum rule option with a default value and exposes it as a property delegate.
 */
public inline fun <reified T : Enum<T>> enumOption(
    name: String,
    defaultValue: T,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<T>> =
    option(name, defaultValue, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Option '$name' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.name }}.")
    }

/**
 * Declares a keyed enum rule option and exposes it as a property delegate.
 */
public inline fun <reified T> keyedEnumOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<T?>>
    where T : Enum<T>, T : KeyedEnum =
    option(name, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.key.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Option '$name' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.key }}.")
    }

/**
 * Declares a keyed enum rule option with a default value and exposes it as a property delegate.
 */
public inline fun <reified T> keyedEnumOption(
    name: String,
    defaultValue: T,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<T>>
    where T : Enum<T>, T : KeyedEnum =
    option(name, defaultValue, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.key.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Option '$name' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.key }}.")
    }

/**
 * Declares a comma-separated enum list rule option and exposes it as a property delegate.
 */
public inline fun <reified T : Enum<T>> enumListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<T>?>> =
    listOption(name, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Option '$name' list item '$value' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.name }}.",
            )
    }

/**
 * Declares a comma-separated enum list rule option with a default value and exposes it as a property delegate.
 */
public inline fun <reified T : Enum<T>> enumListOption(
    name: String,
    defaultValue: List<T>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<T>>> =
    listOption(name, defaultValue, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Option '$name' list item '$value' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.name }}.",
            )
    }

/**
 * Declares a comma-separated keyed enum list rule option and exposes it as a property delegate.
 */
public inline fun <reified T> keyedEnumListOption(
    name: String,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<T>?>>
    where T : Enum<T>, T : KeyedEnum =
    listOption(name, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.key.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Option '$name' list item '$value' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.key }}.",
            )
    }

/**
 * Declares a comma-separated keyed enum list rule option with a default value and exposes it as a property delegate.
 */
public inline fun <reified T> keyedEnumListOption(
    name: String,
    defaultValue: List<T>,
    deprecation: RuleOptionDeprecation? = null,
): ReadOnlyProperty<Rule, RuleOption<List<T>>>
    where T : Enum<T>, T : KeyedEnum =
    listOption(name, defaultValue, deprecation) { value ->
        val entries = enumEntries<T>()
        entries.firstOrNull { enumValue -> enumValue.key.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Option '$name' list item '$value' must be one of ${entries.joinToString(", ") { enumValue -> enumValue.key }}.",
            )
    }

private fun identity(value: String): String = value

private fun nonBlankStringParser(name: String): (String) -> String =
    { value ->
        value.trim().also { parsed ->
            require(parsed.isNotEmpty()) {
                "Option '$name' must be a non-blank string."
            }
        }
    }

private fun booleanParser(name: String): (String) -> Boolean =
    { value ->
        value.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("Option '$name' must be true or false.")
    }

private fun intParser(name: String): (String) -> Int =
    { value ->
        value.toIntOrNull()
            ?: throw IllegalArgumentException("Option '$name' must be an integer.")
    }

private fun longParser(name: String): (String) -> Long =
    { value ->
        value.toLongOrNull()
            ?: throw IllegalArgumentException("Option '$name' must be a long integer.")
    }

private fun positiveIntParser(name: String): (String) -> Int =
    { value ->
        val parsed = value.toIntOrNull()
        require(parsed != null && parsed > 0) {
            "Option '$name' must be a positive integer."
        }
        parsed
    }

private fun nonNegativeIntParser(name: String): (String) -> Int =
    { value ->
        val parsed = value.toIntOrNull()
        require(parsed != null && parsed >= 0) {
            "Option '$name' must be a non-negative integer."
        }
        parsed
    }

private fun <T> commaSeparatedListParser(itemParser: (String) -> T): (String) -> List<T> =
    { value ->
        value
            .split(',')
            .map { item -> item.trim() }
            .filter { item -> item.isNotEmpty() }
            .map(itemParser)
    }

private fun <T : Any> listItemParser(
    name: String,
    itemType: String,
    parser: (String) -> T?,
): (String) -> T =
    { value ->
        parser(value)
            ?: throw IllegalArgumentException("Option '$name' list item '$value' must be a $itemType.")
    }

private fun positiveIntListItemParser(name: String): (String) -> Int =
    { value ->
        val parsed = value.toIntOrNull()
        require(parsed != null && parsed > 0) {
            "Option '$name' list item '$value' must be a positive integer."
        }
        parsed
    }

private fun nonNegativeIntListItemParser(name: String): (String) -> Int =
    { value ->
        val parsed = value.toIntOrNull()
        require(parsed != null && parsed >= 0) {
            "Option '$name' list item '$value' must be a non-negative integer."
        }
        parsed
    }

private class RuleOptionDelegate<T>(
    val option: RuleOption<T>,
) : ReadOnlyProperty<Rule, RuleOption<T>> {
    override fun getValue(
        thisRef: Rule,
        property: KProperty<*>,
    ): RuleOption<T> = option
}

internal fun declaredRuleOptions(rule: Rule): Set<RuleOption<*>> {
    val options =
        generateSequence(rule.javaClass as Class<*>) { type -> type.superclass }
            .flatMap { type -> type.declaredFields.asSequence() }
            .mapNotNull { field ->
                field.isAccessible = true
                (field.get(rule) as? RuleOptionDelegate<*>)?.option
            }
            .toList()
    val duplicateNames =
        options
            .groupingBy { option -> option.name }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    require(duplicateNames.isEmpty()) {
        "Rule option '${duplicateNames.first()}' is already declared."
    }
    return options.toSet()
}
