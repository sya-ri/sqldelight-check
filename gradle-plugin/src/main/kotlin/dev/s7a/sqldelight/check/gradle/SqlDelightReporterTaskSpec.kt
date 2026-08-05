package dev.s7a.sqldelight.check.gradle

import java.io.File
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Gradle-serializable specification for one reporter output configuration.
 *
 * Output files are tracked via the task-level `reportOutputDirectory` property;
 * they are `@Internal` here to avoid declaring outputs on both the spec and the task.
 */
public abstract class SqlDelightReporterTaskSpec {
    /** Reporter ID, e.g. `"json"` or `"sarif"`. */
    @get:Input
    public abstract val name: Property<String>

    /** Whether this reporter should produce output during this task invocation. */
    @get:Input
    public abstract val required: Property<Boolean>

    /**
     * Absolute path of the primary output file for this reporter.
     * Declared `@Internal` because task-level `@OutputDirectory` already covers it.
     */
    @get:Internal
    public abstract val primaryOutputFile: Property<String>

    /**
     * Absolute path of the output directory for reporters that write multiple files.
     * Declared `@Internal` for the same reason as [primaryOutputFile].
     */
    @get:Internal
    public abstract val outputDirectory: Property<String>

    /** Reporter-specific string options passed to the provider. */
    @get:Input
    public abstract val options: MapProperty<String, String>

    internal fun primaryOutputFileAsFile(): File = File(primaryOutputFile.get())

    internal fun outputDirectoryAsFile(): File = File(outputDirectory.get())
}
