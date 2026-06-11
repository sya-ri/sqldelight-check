package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Input required to check one SQLDelight database.
 */
public class AnalysisInput(
    /**
     * Database metadata resolved by the Gradle plugin.
     */
    public val database: DatabaseContext,
    /**
     * Source files that belong to the database.
     */
    public val files: List<SourceFile>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is AnalysisInput &&
            database == other.database &&
            files == other.files

    override fun hashCode(): Int {
        var result = database.hashCode()
        result = 31 * result + files.hashCode()
        return result
    }

    override fun toString(): String =
        "AnalysisInput(database=$database, files=$files)"
}
