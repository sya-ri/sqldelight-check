package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Severity
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
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
         * Rule enablement. Leave unset to let sqldelight-check decide automatically.
         */
        public val enabled: Property<Boolean> =
            objects.property(Boolean::class.java)

        /**
         * Rule severity.
         */
        public val severity: Property<Severity> =
            objects.property(Severity::class.java).convention(Severity.Warning)

        /**
         * Rule-specific string options passed to the rule.
         */
        public val options: MapProperty<String, String> =
            objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

        override fun getName(): String = name
    }
