package dev.s7a.sqldelight.check.rules.standard.rules

internal fun String.isLowerCamelIdentifier(): Boolean =
    isNotEmpty() &&
        first().isLowerCase() &&
        all { character -> character.isLetterOrDigit() } &&
        any { character -> character.isLetter() }
