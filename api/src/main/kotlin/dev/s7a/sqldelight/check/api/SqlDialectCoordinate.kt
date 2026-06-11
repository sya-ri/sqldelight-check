package dev.s7a.sqldelight.check.api

/**
 * Gradle module coordinate for a SQLDelight dialect artifact.
 */
public class SqlDialectCoordinate(
    /**
     * Module group.
     */
    public val group: String,
    /**
     * Module name.
     */
    public val module: String,
    /**
     * Module version when Gradle exposes one.
     */
    public val version: String?,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectCoordinate &&
            group == other.group &&
            module == other.module &&
            version == other.version

    override fun hashCode(): Int {
        var result = group.hashCode()
        result = 31 * result + module.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "SqlDialectCoordinate(group=$group, module=$module, version=$version)"
}
