package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Enablement
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL object for a rule set.
 */
public open class RuleSetExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /** Rule set enablement. */
        public val enabled: Property<Enablement> =
            objects.property(Enablement::class.java).convention(Enablement.Auto)

        override fun getName(): String = name
    }
