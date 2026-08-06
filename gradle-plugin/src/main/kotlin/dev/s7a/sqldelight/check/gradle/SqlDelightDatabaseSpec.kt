package dev.s7a.sqldelight.check.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Gradle-serializable specification for one SQLDelight database input.
 *
 * Populated at configuration time so task actions can run without accessing
 * the project model.
 */
public abstract class SqlDelightDatabaseSpec {
    /**
     * SQLDelight database name (the `className` in the SQLDelight extension).
     */
    @get:Input
    public abstract val name: Property<String>

    /**
     * Dialect artifact coordinate as "group:module:version", or an empty string
     * when the dialect could not be determined.
     */
    @get:Input
    public abstract val dialectCoordinate: Property<String>

    /**
     * Source `.sq` and `.sqm` files that belong to this database.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection
}
