package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * DSL object for one SQLDelight database override.
 */
public open class DatabaseExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /** Database-specific rule set overrides. */
        public val ruleSets: NamedDomainObjectContainer<RuleSetExtension> =
            objects.domainObjectContainer(RuleSetExtension::class.java) { ruleSetName ->
                objects.newInstance(RuleSetExtension::class.java, ruleSetName)
            }

        /** Database-specific rule overrides. */
        public val rules: NamedDomainObjectContainer<RuleExtension> =
            objects.domainObjectContainer(RuleExtension::class.java) { ruleName ->
                objects.newInstance(RuleExtension::class.java, ruleName)
            }

        /**
         * Configures database-specific rule set overrides.
         */
        public fun ruleSets(configure: Action<in NamedDomainObjectContainer<RuleSetExtension>>) {
            configure.execute(ruleSets)
        }

        /**
         * Configures database-specific rule overrides.
         */
        public fun rules(configure: Action<in NamedDomainObjectContainer<RuleExtension>>) {
            configure.execute(rules)
        }

        override fun getName(): String = name
    }
