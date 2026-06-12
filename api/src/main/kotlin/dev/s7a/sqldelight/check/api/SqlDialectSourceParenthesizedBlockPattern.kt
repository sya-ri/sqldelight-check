package dev.s7a.sqldelight.check.api

/**
 * A source block pattern opened and closed by fixed terms.
 *
 * The block receives [innerStartKind] when the first inner token matches one
 * of [innerStartRoles]. Otherwise it receives [defaultKind].
 */
public class SqlDialectSourceParenthesizedBlockPattern(
    public val openTerm: String,
    public val closeTerm: String,
    public val defaultKind: SqlSourceBlockKind,
    public val innerStartRoles: Set<SqlDialectSourcePatternRole> = emptySet(),
    public val innerStartKind: SqlSourceBlockKind = defaultKind,
) {
    public val normalizedOpenTerm: String = openTerm.lowercase()

    public val normalizedCloseTerm: String = closeTerm.lowercase()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourceParenthesizedBlockPattern &&
            openTerm == other.openTerm &&
            closeTerm == other.closeTerm &&
            defaultKind == other.defaultKind &&
            innerStartRoles == other.innerStartRoles &&
            innerStartKind == other.innerStartKind

    override fun hashCode(): Int {
        var result = openTerm.hashCode()
        result = 31 * result + closeTerm.hashCode()
        result = 31 * result + defaultKind.hashCode()
        result = 31 * result + innerStartRoles.hashCode()
        result = 31 * result + innerStartKind.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialectSourceParenthesizedBlockPattern(openTerm=$openTerm, closeTerm=$closeTerm, " +
            "defaultKind=$defaultKind, innerStartRoles=$innerStartRoles, innerStartKind=$innerStartKind)"
}
