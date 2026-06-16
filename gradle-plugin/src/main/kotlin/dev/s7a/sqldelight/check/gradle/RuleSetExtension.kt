package dev.s7a.sqldelight.check.gradle

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
        /**
         * Rule set enablement. Leave unset to let sqldelight-check decide automatically.
         */
        public val enabled: Property<Boolean> =
            objects.property(Boolean::class.java)

        override fun getName(): String = name
    }
