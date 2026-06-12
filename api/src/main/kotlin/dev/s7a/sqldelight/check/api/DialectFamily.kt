package dev.s7a.sqldelight.check.api

/**
 * Database family for SQL dialects.
 *
 * Dialect modules can define their own implementation when they need a stable
 * family identity, or use [Named] for a string-backed identity.
 */
public interface DialectFamily {
    /**
     * Stable identifier for this dialect family.
     */
    public val id: String

    /**
     * Dialect family used when no provider resolves a dialect artifact.
     */
    public data object Unknown : DialectFamily {
        override val id: String = "unknown"
    }

    /**
     * Named dialect family for integrations that do not need their own type.
     */
    public class Named(
        override val id: String,
    ) : DialectFamily {
        override fun equals(other: Any?): Boolean =
            this === other ||
                other is Named &&
                id == other.id

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String = "DialectFamily.Named(id=$id)"
    }
}
