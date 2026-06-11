package dev.s7a.sqldelight.check.gradle

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * DSL object for the built-in HTML reporter.
 */
public open class HtmlReporterExtension
    @Inject
    constructor(
        name: String,
        objects: ObjectFactory,
    ) : ReporterExtension(name, objects)
