package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Named
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL object for one reporter.
 */
public open class ReporterExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /** Whether this reporter should produce output. */
        public val required: Property<Boolean> =
            objects.property(Boolean::class.java).convention(false)

        /** Report output file. */
        public val outputFile: RegularFileProperty = objects.fileProperty()

        /** Reporter-specific string options passed to the selected reporter provider. */
        public val options: MapProperty<String, String> =
            objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

        override fun getName(): String = name
    }
