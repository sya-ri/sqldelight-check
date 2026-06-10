package dev.s7a.sqldelight.check.adapter.v232

/**
 * Stable SQLDelight 2.x version range supported by the 2.3.2 adapter implementation.
 */
internal object SqlDelight2VersionSupport {
    private val stableVersion = Regex("""^2\.(\d+)\.\d+$""")

    fun supports(version: String): Boolean {
        val minor = stableVersion.matchEntire(version)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return minor in 0..4
    }
}
