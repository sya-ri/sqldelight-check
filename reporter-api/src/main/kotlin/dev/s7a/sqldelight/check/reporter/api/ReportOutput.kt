package dev.s7a.sqldelight.check.reporter.api

import java.io.OutputStream

/**
 * Output target for reporter files.
 *
 * Reporters that produce one artifact should use [file]. Reporters that need
 * additional assets or multiple shards should use [file] with stable relative
 * paths.
 */
public interface ReportOutput {
    /**
     * Opens the reporter's primary output file.
     */
    public fun file(): OutputStream

    /**
     * Opens a file below the reporter's output directory.
     */
    public fun file(path: String): OutputStream
}
