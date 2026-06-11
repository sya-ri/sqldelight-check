package dev.s7a.sqldelight.check.api

/**
 * Lightweight SQLDelight database context visible to rules and reporters.
 */
public class DatabaseContext(
    /**
     * SQLDelight database name.
     */
    public val name: String,
    /**
     * Dialect used to analyze files in this database.
     */
    public val dialect: SqlDialect,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is DatabaseContext &&
            name == other.name &&
            dialect == other.dialect

    override fun hashCode(): Int = 31 * name.hashCode() + dialect.hashCode()

    override fun toString(): String = "DatabaseContext(name=$name, dialect=$dialect)"
}
