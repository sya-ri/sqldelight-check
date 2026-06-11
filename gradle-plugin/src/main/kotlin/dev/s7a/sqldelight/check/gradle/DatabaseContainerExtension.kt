package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Provides named accessors for database-specific override configuration.
 *
 * This wrapper keeps the public Gradle DSL focused on stable helper methods
 * while preserving the underlying named container for compatibility.
 */
public class DatabaseContainerExtension(
    private val elements: NamedDomainObjectContainer<DatabaseExtension>,
) : Iterable<DatabaseExtension> {
    /**
     * Creates or configures database-specific overrides by SQLDelight database
     * name.
     */
    public fun database(
        name: String,
        configure: Action<in DatabaseExtension>,
    ) {
        configure.execute(maybeCreate(name))
    }

    /**
     * Creates or returns database-specific overrides by SQLDelight database
     * name without applying additional configuration.
     */
    public fun maybeCreate(name: String): DatabaseExtension = elements.maybeCreate(name)

    override fun iterator(): Iterator<DatabaseExtension> = elements.iterator()
}
