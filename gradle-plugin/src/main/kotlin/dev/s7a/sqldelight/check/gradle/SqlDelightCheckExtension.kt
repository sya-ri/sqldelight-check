package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Severity
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle DSL for configuring sqldelight-check.
 */
public open class SqlDelightCheckExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** Rule set defaults applied to every SQLDelight database unless overridden. */
        public val ruleSets: NamedDomainObjectContainer<RuleSetExtension> =
            objects.domainObjectContainer(RuleSetExtension::class.java) { name ->
                objects.newInstance(RuleSetExtension::class.java, name)
            }

        /** Rule overrides applied to every SQLDelight database unless overridden. */
        public val rules: NamedDomainObjectContainer<RuleExtension> =
            objects.domainObjectContainer(RuleExtension::class.java) { name ->
                objects.newInstance(RuleExtension::class.java, name)
            }

        /** Reporters and output options. */
        public val reports: NamedDomainObjectContainer<ReporterExtension> =
            objects.domainObjectContainer(ReporterExtension::class.java) { name ->
                objects.newInstance(ReporterExtension::class.java, name)
            }

        /** Database-specific overrides keyed by SQLDelight database name. */
        public val databases: NamedDomainObjectContainer<DatabaseExtension> =
            objects.domainObjectContainer(DatabaseExtension::class.java) { name ->
                objects.newInstance(DatabaseExtension::class.java, name)
            }

        /** Write behavior shared by `*Write` tasks. */
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

/**
 * DSL object for one rule override.
 */
public open class RuleExtension
    @Inject
    constructor(
        private val name: String,
        objects: ObjectFactory,
    ) : Named {
        /** Rule enablement. */
        public val enabled: Property<Enablement> =
            objects.property(Enablement::class.java).convention(Enablement.Auto)

        /** Rule severity. */
        public val severity: Property<Severity> =
            objects.property(Severity::class.java).convention(Severity.Warning)

        override fun getName(): String = name
    }

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

        override fun getName(): String = name
    }

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

/**
 * DSL object for write task behavior.
 */
public open class WriteExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** Whether write tasks may apply unsafe fixes. */
        public val unsafe: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    }
