package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile
import java.io.File

/**
 * Input required to analyze one SQLDelight database.
 */
public data class AnalysisInput(
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
)
