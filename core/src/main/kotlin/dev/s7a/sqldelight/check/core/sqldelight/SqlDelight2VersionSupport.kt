package dev.s7a.sqldelight.check.core.sqldelight

/**
 * Stable SQLDelight 2.x version range supported by the built-in analyzer.
 */
internal object SqlDelight2VersionSupport {
    fun supports(version: String): Boolean {
        val parsed = SqlDelightVersion.parse(version) ?: return false
        return parsed.isStableSupported() || parsed.isPreviewSupported()
    }

    private fun SqlDelightVersion.isStableSupported(): Boolean =
        major == SUPPORTED_MAJOR && minor in SUPPORTED_STABLE_MINOR_RANGE && qualifier == null

    private fun SqlDelightVersion.isPreviewSupported(): Boolean =
        major == SUPPORTED_MAJOR && minor == PREVIEW_MINOR && patch == PREVIEW_PATCH && qualifier == PREVIEW_QUALIFIER

    private const val SUPPORTED_MAJOR = 2
    private val SUPPORTED_STABLE_MINOR_RANGE = 0..3
    private const val PREVIEW_MINOR = 4
    private const val PREVIEW_PATCH = 0
    private const val PREVIEW_QUALIFIER = "SNAPSHOT"
}
