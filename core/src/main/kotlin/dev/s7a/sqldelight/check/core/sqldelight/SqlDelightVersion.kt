package dev.s7a.sqldelight.check.core.sqldelight

/**
 * Parsed SQLDelight semantic version used for compatibility checks.
 */
internal data class SqlDelightVersion(
    /** Major version component. */
    val major: Int,
    /** Minor version component. */
    val minor: Int,
    /** Patch version component. */
    val patch: Int,
    /** Optional prerelease qualifier without the leading hyphen. */
    val qualifier: String?,
) {
    companion object {
        private val versionPattern = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9.-]+))?$""")

        /**
         * Parses a SQLDelight version string supported by Gradle plugin coordinates.
         */
        fun parse(version: String): SqlDelightVersion? {
            val match = versionPattern.matchEntire(version) ?: return null
            return SqlDelightVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                qualifier = match.groupValues[4].takeIf { value -> value.isNotEmpty() },
            )
        }
    }
}
