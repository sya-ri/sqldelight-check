package dev.s7a.sqldelight.check.gradle

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * DSL object for the built-in GitHub annotations reporter.
 */
public open class GitHubAnnotationsReporterExtension
    @Inject
    constructor(
        name: String,
        objects: ObjectFactory,
    ) : ReporterExtension(name, objects)
