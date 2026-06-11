package dev.s7a.sqldelight.check.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL object for fix task behavior.
 */
public open class FixExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Whether fix tasks may apply unsafe fixes.
         */
        public val unsafe: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    }
