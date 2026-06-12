package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Provides named accessors for rule set default configuration.
 *
 * Built-in rule sets have typed helper methods, and third-party rule sets can
 * still be configured through [ruleSet].
 */
public class RuleSetContainerExtension(
    private val elements: NamedDomainObjectContainer<RuleSetExtension>,
) : Iterable<RuleSetExtension> {
    /**
     * Creates or configures a rule set by its Gradle DSL ID.
     *
     * External rule set IDs are supported through this generic entry point.
     */
    public fun ruleSet(
        name: String,
        configure: Action<in RuleSetExtension>,
    ) {
        configure.execute(maybeCreate(name))
    }

    /**
     * Creates or returns a rule set by its Gradle DSL ID without applying
     * additional configuration.
     */
    public fun maybeCreate(name: String): RuleSetExtension = elements.maybeCreate(name)

    /**
     * Configures the built-in standard rule set shared by all SQL dialects.
     *
     * This rule set is installed by default with the Gradle plugin.
     */
    public fun standard(configure: Action<in RuleSetExtension>) {
        ruleSet("standard", configure)
    }

    /**
     * Configures the built-in PostgreSQL rule set.
     *
     * Rules in this set stay inactive under `Auto` unless the database exposes
     * PostgreSQL dialect IDs.
     */
    public fun postgres(configure: Action<in RuleSetExtension>) {
        ruleSet("postgres", configure)
    }

    /**
     * Configures the built-in MySQL rule set.
     *
     * Rules in this set stay inactive under `Auto` unless the database exposes
     * MySQL dialect IDs.
     */
    public fun mysql(configure: Action<in RuleSetExtension>) {
        ruleSet("mysql", configure)
    }

    /**
     * Configures the built-in SQLite rule set.
     *
     * Rules in this set stay inactive under `Auto` unless the database exposes
     * SQLite dialect IDs.
     */
    public fun sqlite(configure: Action<in RuleSetExtension>) {
        ruleSet("sqlite", configure)
    }

    /**
     * Configures the built-in HSQL rule set.
     *
     * Rules in this set stay inactive under `Auto` unless the database exposes
     * HSQL dialect IDs.
     */
    public fun hsql(configure: Action<in RuleSetExtension>) {
        ruleSet("hsql", configure)
    }

    override fun iterator(): Iterator<RuleSetExtension> = elements.iterator()
}
