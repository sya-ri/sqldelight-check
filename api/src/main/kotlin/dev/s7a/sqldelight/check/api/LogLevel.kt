package dev.s7a.sqldelight.check.api

/**
 * Controls how much execution detail sqldelight-check should emit.
 *
 * The levels are cumulative:
 * `Verbose` includes `Info`, and `Debug` includes `Verbose`.
 */
public enum class LogLevel {
    /**
     * Emits only the task summary.
     */
    Info,

    /**
     * Emits the resolved file inventory in addition to the summary.
     */
    Verbose,

    /**
     * Emits the resolved file inventory and per-file rule execution details.
     */
    Debug,
    ;

    /**
     * Returns whether this level should emit resolved file inventory logs.
     */
    public val logsFiles: Boolean
        get() = this >= Verbose

    /**
     * Returns whether this level should emit per-file rule execution logs.
     */
    public val logsRules: Boolean
        get() = this >= Debug
}
