package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Top-level Gradle DSL for configuring sqldelight-check.
 *
 * The extension owns global defaults, database-specific overrides, reporter
 * output settings, and fix safety options.
 */
public open class SqlDelightCheckExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Rule set defaults applied to every SQLDelight database unless
         * database-specific overrides replace them.
         */
        public val ruleSets: NamedDomainObjectContainer<RuleSetExtension> =
            objects.domainObjectContainer(RuleSetExtension::class.java) { name ->
                objects.newInstance(RuleSetExtension::class.java, name)
            }

        /**
         * Rule overrides applied to every SQLDelight database unless
         * database-specific overrides replace them.
         */
        public val rules: NamedDomainObjectContainer<RuleExtension> =
            objects.domainObjectContainer(RuleExtension::class.java) { name ->
                objects.newInstance(RuleExtension::class.java, name)
            }

        /**
         * Reporters and output options keyed by reporter ID.
         *
         * Built-in reporters are registered by the plugin before task execution.
         */
        public val reports: NamedDomainObjectContainer<ReporterExtension> =
            objects.domainObjectContainer(ReporterExtension::class.java) { name ->
                when (name) {
                    "github-annotations" -> objects.newInstance(GitHubAnnotationsReporterExtension::class.java, name)
                    "html" -> objects.newInstance(HtmlReporterExtension::class.java, name)
                    "json" -> objects.newInstance(JsonReporterExtension::class.java, name)
                    "markdown" -> objects.newInstance(MarkdownReporterExtension::class.java, name)
                    "sarif" -> objects.newInstance(SarifReporterExtension::class.java, name)
                    "text" -> objects.newInstance(TextReporterExtension::class.java, name)
                    else -> objects.newInstance(ReporterExtension::class.java, name)
                }
            }

        /**
         * Database-specific overrides keyed by SQLDelight database name.
         *
         * These values are merged after top-level rule and rule set defaults.
         */
        public val databases: NamedDomainObjectContainer<DatabaseExtension> =
            objects.domainObjectContainer(DatabaseExtension::class.java) { name ->
                objects.newInstance(DatabaseExtension::class.java, name)
            }

        /**
         * Fix behavior shared by tasks that modify SQLDelight sources.
         */
        public val fix: FixExtension = objects.newInstance(FixExtension::class.java)

        /**
         * Controls how much execution detail the task emits.
         *
         * `Info` keeps the output to summaries, `Verbose` adds resolved files,
         * and `Debug` adds per-file rule traces.
         */
        public val logLevel: Property<LogLevel> =
            objects.property(LogLevel::class.java).convention(LogLevel.Info)

        private val ruleSetsDsl: RuleSetContainerExtension = RuleSetContainerExtension(ruleSets)
        private val rulesDsl: RuleContainerExtension = RuleContainerExtension(rules)
        private val reportsDsl: ReporterContainerExtension = ReporterContainerExtension(reports)
        private val databasesDsl: DatabaseContainerExtension = DatabaseContainerExtension(databases)

        /**
         * Configures rule set defaults using the nested DSL.
         *
         * The container also exposes `maybeCreate` for compatibility.
         */
        public fun ruleSets(configure: Action<in RuleSetContainerExtension>) {
            configure.execute(ruleSetsDsl)
        }

        /**
         * Configures rule overrides using the nested DSL.
         *
         * Use `rule("rule-set:rule-id")` for built-in and external rules.
         */
        public fun rules(configure: Action<in RuleContainerExtension>) {
            configure.execute(rulesDsl)
        }

        /**
         * Configures reporters using the nested DSL.
         *
         * Built-in reporters have named helpers and external reporters use `report`.
         */
        public fun reports(configure: Action<in ReporterContainerExtension>) {
            configure.execute(reportsDsl)
        }

        /**
         * Configures database-specific overrides using the nested DSL.
         *
         * Use `database("Name")` with the SQLDelight database name.
         */
        public fun databases(configure: Action<in DatabaseContainerExtension>) {
            configure.execute(databasesDsl)
        }

        /**
         * Configures fix behavior for tasks that modify SQLDelight sources.
         */
        public fun fix(configure: Action<in FixExtension>) {
            configure.execute(fix)
        }
    }
