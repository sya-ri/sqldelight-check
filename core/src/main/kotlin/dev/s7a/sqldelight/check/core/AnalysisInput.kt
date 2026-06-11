package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile
import java.io.File

/**
 * Input required to analyze one SQLDelight database.
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
    /**
     * SQLDelight version used by the database.
     */
    public val sqlDelightVersion: String? = null,
    /**
     * SQLDelight package name configured for the database.
     */
    public val packageName: String? = null,
    /**
     * Source folders that SQLDelight should parse for this database.
     */
    public val sourceFolders: List<File> = emptyList(),
    /**
     * Dependency source folders from other SQLDelight databases.
     */
    public val dependencyFolders: List<File> = emptyList(),
    /**
     * SQLDelight compiler/runtime classpath used by the original SQLDelight Gradle task.
     */
    public val compilerClasspath: List<File> = emptyList(),
    /**
     * Runtime classpath used for loading the configured SQLDelight dialect.
     */
    public val dialectClasspath: List<File> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is AnalysisInput &&
            database == other.database &&
            files == other.files &&
            sqlDelightVersion == other.sqlDelightVersion &&
            packageName == other.packageName &&
            sourceFolders == other.sourceFolders &&
            dependencyFolders == other.dependencyFolders &&
            compilerClasspath == other.compilerClasspath &&
            dialectClasspath == other.dialectClasspath

    override fun hashCode(): Int {
        var result = database.hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + (sqlDelightVersion?.hashCode() ?: 0)
        result = 31 * result + (packageName?.hashCode() ?: 0)
        result = 31 * result + sourceFolders.hashCode()
        result = 31 * result + dependencyFolders.hashCode()
        result = 31 * result + compilerClasspath.hashCode()
        result = 31 * result + dialectClasspath.hashCode()
        return result
    }

    override fun toString(): String =
        "AnalysisInput(database=$database, files=$files, sqlDelightVersion=$sqlDelightVersion, packageName=$packageName, sourceFolders=$sourceFolders, dependencyFolders=$dependencyFolders, compilerClasspath=$compilerClasspath, dialectClasspath=$dialectClasspath)"
}
