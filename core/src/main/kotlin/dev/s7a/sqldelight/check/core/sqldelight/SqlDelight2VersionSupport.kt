package dev.s7a.sqldelight.check.core.sqldelight

/**
 * Stable SQLDelight 2.x version range supported by the built-in analyzer.
 */
internal object SqlDelight2VersionSupport {
    private val stableVersion = Regex("""^2\.(\d+)\.\d+$""")

    fun supports(version: String): Boolean {
        if (version == "2.4.0-SNAPSHOT") return true
        val minor = stableVersion.matchEntire(version)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return minor in 0..3
    }
}
