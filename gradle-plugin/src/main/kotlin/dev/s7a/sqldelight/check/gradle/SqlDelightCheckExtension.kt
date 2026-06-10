package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Gradle DSL for configuring sqldelight-check.
 */
public open class SqlDelightCheckExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Rule set defaults applied to every SQLDelight database unless overridden.
         */
        public val ruleSets: NamedDomainObjectContainer<RuleSetExtension> =
            objects.domainObjectContainer(RuleSetExtension::class.java) { name ->
                objects.newInstance(RuleSetExtension::class.java, name)
            }

        /**
         * Rule overrides applied to every SQLDelight database unless overridden.
         */
        public val rules: NamedDomainObjectContainer<RuleExtension> =
            objects.domainObjectContainer(RuleExtension::class.java) { name ->
                objects.newInstance(RuleExtension::class.java, name)
            }

        /**
         * Reporters and output options.
         */
        public val reports: NamedDomainObjectContainer<ReporterExtension> =
            objects.domainObjectContainer(ReporterExtension::class.java) { name ->
                objects.newInstance(ReporterExtension::class.java, name)
            }

        /**
         * Database-specific overrides keyed by SQLDelight database name.
         */
        public val databases: NamedDomainObjectContainer<DatabaseExtension> =
            objects.domainObjectContainer(DatabaseExtension::class.java) { name ->
                objects.newInstance(DatabaseExtension::class.java, name)
            }

        /**
         * Write behavior shared by `*Write` tasks.
         */
        public val write: WriteExtension = objects.newInstance(WriteExtension::class.java)

        /**
         * Configures a rule set default.
         */
        public fun ruleSets(configure: Action<in NamedDomainObjectContainer<RuleSetExtension>>) {
            configure.execute(ruleSets)
        }

        /**
         * Configures rule overrides.
         */
        public fun rules(configure: Action<in NamedDomainObjectContainer<RuleExtension>>) {
            configure.execute(rules)
        }

        /**
         * Configures reporters.
         */
        public fun reports(configure: Action<in NamedDomainObjectContainer<ReporterExtension>>) {
            configure.execute(reports)
        }

        /**
         * Configures database-specific overrides.
         */
        public fun databases(configure: Action<in NamedDomainObjectContainer<DatabaseExtension>>) {
            configure.execute(databases)
        }

        /**
         * Configures write behavior.
         */
        public fun write(configure: Action<in WriteExtension>) {
            configure.execute(write)
        }
    }
