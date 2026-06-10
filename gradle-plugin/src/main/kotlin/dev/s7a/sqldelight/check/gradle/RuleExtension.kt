package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Severity
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL object for one rule override.
 */
public open class RuleExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /**
         * Rule enablement.
         */
        public val enabled: Property<Enablement> =
            objects.property(Enablement::class.java).convention(Enablement.Auto)

        /**
         * Rule severity.
         */
        public val severity: Property<Severity> =
            objects.property(Severity::class.java).convention(Severity.Warning)

        override fun getName(): String = name
    }
