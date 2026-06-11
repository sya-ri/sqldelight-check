package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Provides named accessors for rule override configuration.
 *
 * Rule IDs are intentionally passed as strings so external rule sets can use
 * the same DSL surface as built-in rules.
 */
public class RuleContainerExtension(
    private val elements: NamedDomainObjectContainer<RuleExtension>,
) : Iterable<RuleExtension> {
    /**
     * Creates or configures a rule override by its full rule ID.
     *
     * Full IDs use the `rule-set:rule-name` form.
     */
    public fun rule(
        name: String,
        configure: Action<in RuleExtension>,
    ) {
        configure.execute(maybeCreate(name))
    }

    /**
     * Creates or returns a rule override by its full rule ID without applying
     * additional configuration.
     */
    public fun maybeCreate(name: String): RuleExtension = elements.maybeCreate(name)

    override fun iterator(): Iterator<RuleExtension> = elements.iterator()
}
