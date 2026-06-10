package dev.s7a.sqldelight.check.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Base task for sqldelight-check operations.
 */
public abstract class SqlDelightCheckTask : DefaultTask() {
    /**
     * Runs the placeholder task implementation.
     *
     * FIXME: Connect tasks to SQLDelight project detection, core execution, reports, and write handling.
     */
    @TaskAction
    public fun run() {
        logger.lifecycle("{} is not wired to analysis yet.", path)
    }
}

