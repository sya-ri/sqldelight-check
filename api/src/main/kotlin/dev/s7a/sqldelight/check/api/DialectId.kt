package dev.s7a.sqldelight.check.api

/**
 * SQL dialect identifier used by rules and reports without depending on artifact names.
 */
public class DialectId(
    /**
     * Stable dialect ID.
     */
    public val id: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is DialectId &&
            id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "DialectId(id=$id)"

    public companion object {
        /**
         * Dialect ID used when no provider resolves a dialect artifact.
         */
        public val Unknown: DialectId = DialectId("unknown")
    }
}
