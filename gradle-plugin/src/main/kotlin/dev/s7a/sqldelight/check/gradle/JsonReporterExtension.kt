package dev.s7a.sqldelight.check.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL object for the built-in JSON reporter.
 */
public open class JsonReporterExtension
    @Inject
    constructor(
        name: String,
        objects: ObjectFactory,
    ) : ReporterExtension(name, objects) {
        /**
         * Whether the JSON reporter should format output for humans.
         */
        public val prettyPrint: Property<Boolean> =
            objects.property(Boolean::class.java)

        override fun resolvedOptions(): Map<String, String> =
            buildMap {
                putAll(super.resolvedOptions())
                if (prettyPrint.isPresent) {
                    put(PRETTY_PRINT_OPTION, prettyPrint.get().toString())
                }
            }
    }
