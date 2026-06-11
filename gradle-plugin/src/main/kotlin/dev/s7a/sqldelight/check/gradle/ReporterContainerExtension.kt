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
    public fun json(configure: Action<in JsonReporterExtension>) {
        val reporter = maybeCreate("json")
        require(reporter is JsonReporterExtension) {
            "Reporter 'json' must use JsonReporterExtension."
        }
        configure.execute(reporter)
    }

    /**
     * Configures the built-in SARIF reporter used by code scanning tools.
     *
     * The generated report follows the SARIF 2.1.0 schema.
     */
    public fun sarif(configure: Action<in SarifReporterExtension>) {
        val reporter = maybeCreate("sarif")
        require(reporter is SarifReporterExtension) {
            "Reporter 'sarif' must use SarifReporterExtension."
        }
        configure.execute(reporter)
    }

    /**
     * Configures the built-in text reporter used for compact console-friendly
     * output.
     */
    public fun text(configure: Action<in TextReporterExtension>) {
        val reporter = maybeCreate("text")
        require(reporter is TextReporterExtension) {
            "Reporter 'text' must use TextReporterExtension."
        }
        configure.execute(reporter)
    }

    /**
     * Configures the built-in HTML reporter used for navigable CI artifacts.
     *
     * The report includes grouped diagnostics and source excerpts.
     */
    public fun html(configure: Action<in HtmlReporterExtension>) {
        val reporter = maybeCreate("html")
        require(reporter is HtmlReporterExtension) {
            "Reporter 'html' must use HtmlReporterExtension."
        }
        configure.execute(reporter)
    }

    /**
     * Configures the built-in Markdown reporter used for GitHub Actions job
     * summaries.
     */
    public fun markdown(configure: Action<in MarkdownReporterExtension>) {
        val reporter = maybeCreate("markdown")
        require(reporter is MarkdownReporterExtension) {
            "Reporter 'markdown' must use MarkdownReporterExtension."
        }
        configure.execute(reporter)
    }

    /**
     * Configures the built-in GitHub annotations reporter used for workflow
     * command output.
     */
    public fun githubAnnotations(configure: Action<in GitHubAnnotationsReporterExtension>) {
        val reporter = maybeCreate("github-annotations")
        require(reporter is GitHubAnnotationsReporterExtension) {
            "Reporter 'github-annotations' must use GitHubAnnotationsReporterExtension."
        }
        configure.execute(reporter)
    }

    override fun iterator(): Iterator<ReporterExtension> = elements.iterator()
}
