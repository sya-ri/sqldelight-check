package dev.s7a.sqldelight.check.api

/**
 * Dialect capability identifier used by rules to check feature support without depending on artifact names.
 */
public class DialectCapability(
    /**
     * Stable capability ID.
     */
    public val id: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is DialectCapability &&
            id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "DialectCapability(id=$id)"
}
