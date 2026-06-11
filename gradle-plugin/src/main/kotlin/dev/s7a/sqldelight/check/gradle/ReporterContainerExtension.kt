package dev.s7a.sqldelight.check.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Provides named accessors for reporter configuration.
 *
 * Built-in reporters have typed helper methods, and third-party reporters can
 * still be configured through [report].
 */
public class ReporterContainerExtension(
    private val elements: NamedDomainObjectContainer<ReporterExtension>,
) : Iterable<ReporterExtension> {
    /**
     * Creates or configures a reporter by its Gradle DSL ID.
     *
     * External reporter IDs are supported through this generic entry point.
     */
    public fun report(
        name: String,
        configure: Action<in ReporterExtension>,
    ) {
        configure.execute(maybeCreate(name))
    }

    /**
     * Creates or returns a reporter by its Gradle DSL ID without applying
     * additional configuration.
     */
    public fun maybeCreate(name: String): ReporterExtension = elements.maybeCreate(name)

    /**
     * Configures the built-in JSON reporter used for machine-readable report
     * output.
     */
    public fun json(configure: Action<in ReporterExtension>) {
        report("json", configure)
    }

    /**
     * Configures the built-in SARIF reporter used by code scanning tools.
     *
     * The generated report follows the SARIF 2.1.0 schema.
     */
    public fun sarif(configure: Action<in ReporterExtension>) {
        report("sarif", configure)
    }

    /**
     * Configures the built-in text reporter used for compact console-friendly
     * output.
     */
    public fun text(configure: Action<in ReporterExtension>) {
        report("text", configure)
    }

    /**
     * Configures the built-in HTML reporter used for navigable CI artifacts.
     *
     * The report includes grouped diagnostics and source excerpts.
     */
    public fun html(configure: Action<in ReporterExtension>) {
        report("html", configure)
    }

    /**
     * Configures the built-in Markdown reporter used for GitHub Actions job
     * summaries.
     */
    public fun markdown(configure: Action<in ReporterExtension>) {
        report("markdown", configure)
    }

    /**
     * Configures the built-in GitHub annotations reporter used for workflow
     * command output.
     */
    public fun githubAnnotations(configure: Action<in ReporterExtension>) {
        report("github-annotations", configure)
    }

    override fun iterator(): Iterator<ReporterExtension> = elements.iterator()
}
