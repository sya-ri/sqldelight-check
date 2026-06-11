package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Configures sqldelight-check overrides for one SQLDelight database.
 *
 * Database-specific values override the top-level DSL only for the matching
 * SQLDelight database name.
 */
public open class DatabaseExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /**
         * Database-specific rule set overrides keyed by rule set ID.
         *
         * These values replace top-level rule set defaults for this database.
         */
        public val ruleSets: NamedDomainObjectContainer<RuleSetExtension> =
            objects.domainObjectContainer(RuleSetExtension::class.java) { ruleSetName ->
                objects.newInstance(RuleSetExtension::class.java, ruleSetName)
            }

        /**
         * Database-specific rule overrides keyed by full rule ID.
         *
         * These values replace top-level rule overrides for this database.
         */
        public val rules: NamedDomainObjectContainer<RuleExtension> =
            objects.domainObjectContainer(RuleExtension::class.java) { ruleName ->
                objects.newInstance(RuleExtension::class.java, ruleName)
            }

        private val ruleSetsDsl: RuleSetContainerExtension = RuleSetContainerExtension(ruleSets)
        private val rulesDsl: RuleContainerExtension = RuleContainerExtension(rules)

        /**
         * Configures database-specific rule set overrides using the nested DSL.
         *
         * The nested container has the same helpers as the top-level rule sets block.
         */
        public fun ruleSets(configure: Action<in RuleSetContainerExtension>) {
            configure.execute(ruleSetsDsl)
        }

        /**
         * Configures database-specific rule overrides using the nested DSL.
         *
         * Use this for database-local severity, enablement, or option values.
         */
        public fun rules(configure: Action<in RuleContainerExtension>) {
            configure.execute(rulesDsl)
        }

        override fun getName(): String = name
    }
