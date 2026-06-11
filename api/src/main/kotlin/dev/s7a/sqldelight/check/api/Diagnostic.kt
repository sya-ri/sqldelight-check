package dev.s7a.sqldelight.check.api

/**
 * Diagnostic emitted by a rule, core analyzer, formatter, or configuration validation.
 */
public class Diagnostic(
    /**
     * Rule ID responsible for the diagnostic when available.
     */
    public val ruleId: RuleId?,
    /**
     * Resolved severity.
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
     * Rule ID after the engine combines [ruleId] with the contributing rule set.
     */
    public val qualifiedRuleId: QualifiedRuleId? = null,
    /**
     * Optional fixes for the diagnostic.
     */
    public val fixes: List<Fix> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Diagnostic &&
            ruleId == other.ruleId &&
            severity == other.severity &&
            message == other.message &&
            file == other.file &&
            range == other.range &&
            database == other.database &&
            qualifiedRuleId == other.qualifiedRuleId &&
            fixes == other.fixes

    override fun hashCode(): Int {
        var result = ruleId?.hashCode() ?: 0
        result = 31 * result + severity.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + (range?.hashCode() ?: 0)
        result = 31 * result + (database?.hashCode() ?: 0)
        result = 31 * result + (qualifiedRuleId?.hashCode() ?: 0)
        result = 31 * result + fixes.hashCode()
        return result
    }

    override fun toString(): String =
        "Diagnostic(ruleId=$ruleId, severity=$severity, message=$message, file=$file, range=$range, database=$database, qualifiedRuleId=$qualifiedRuleId, fixes=$fixes)"
}
