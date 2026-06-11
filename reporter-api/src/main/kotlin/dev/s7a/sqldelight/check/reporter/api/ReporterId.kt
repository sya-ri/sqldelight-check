package dev.s7a.sqldelight.check.reporter.api

/**
 * Identifies a reporter provider in the Gradle DSL.
 */
public data class ReporterId(
    /**
     * Stable reporter ID value, such as `json` or `github-annotations`.
     */
    public val value: String,
) {
    init {
        require(value.matches(ReporterIdPattern)) {
            "Reporter ID must use lowercase kebab-case with letters and digits: $value"
        }
    }

    override fun toString(): String = value

    private companion object {
        private val ReporterIdPattern = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    }
}
