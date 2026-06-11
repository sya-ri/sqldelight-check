package dev.s7a.sqldelight.check.api

/**
 * Diagnostic emitted by a rule before the engine attaches the containing rule set.
 */
public class RuleDiagnostic(
    /**
     * Severity requested by the rule before user configuration overrides it.
     */
    public val severity: Severity,
    /**
     * User-facing message.
     */
    public val message: String,
    /**
     * File where the diagnostic occurred.
     */
    public val file: SourceFile?,
    /**
     * Source range where the diagnostic occurred.
     */
    public val range: SourceRange?,
    /**
     * Database context associated with the diagnostic when known.
     */
    public val database: DatabaseContext?,
    /**
     * Optional fixes for the diagnostic.
     */
    public val fixes: List<Fix> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleDiagnostic &&
            severity == other.severity &&
            message == other.message &&
            file == other.file &&
            range == other.range &&
            database == other.database &&
            fixes == other.fixes

    override fun hashCode(): Int {
        var result = severity.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + (range?.hashCode() ?: 0)
        result = 31 * result + (database?.hashCode() ?: 0)
        result = 31 * result + fixes.hashCode()
        return result
    }

    override fun toString(): String =
        "RuleDiagnostic(severity=$severity, message=$message, file=$file, range=$range, database=$database, fixes=$fixes)"
}
