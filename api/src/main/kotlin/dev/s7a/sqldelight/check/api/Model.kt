package dev.s7a.sqldelight.check.api

/**
 * Severity assigned to a diagnostic after rule defaults and user configuration are resolved.
 */
public enum class Severity {
    /** Informational diagnostic that does not normally fail builds. */
    Info,

    /** Warning diagnostic that should be visible but does not fail builds by default. */
    Warning,

    /** Error diagnostic that should fail check tasks. */
    Error,
}

/**
 * User-facing enablement state for rules and rule sets.
 */
public enum class Enablement {
    /** Let sqldelight-check decide from database, dialect, SQLDelight version, and rule applicability. */
    Auto,

    /** Explicitly enable the rule or rule set. */
    Enabled,

    /** Explicitly disable the rule or rule set. */
    Disabled,
}

/**
 * Safety classification for a write operation proposed by a rule or formatter.
 */
public enum class FixSafety {
    /** The edit is expected to preserve behavior and may run during normal write tasks. */
    Safe,

    /** The edit may change behavior and requires explicit user opt-in. */
    Unsafe,
}

/**
 * Identifies a rule set such as `standard`, `sqlite`, or a third-party provider ID.
 */
@JvmInline
public value class RuleSetId(
    public val value: String,
)

/**
 * Identifies a rule using the `rule-set:rule-name` form.
 */
@JvmInline
public value class RuleId(
    public val value: String,
)

/**
 * One-based source position for diagnostics and edits.
 */
public data class SourcePosition(
    /** One-based line number. */
    public val line: Int,
    /** One-based column number. */
    public val column: Int,
)

/**
 * Source range in a file.
 */
public data class SourceRange(
    /** Inclusive start position. */
    public val start: SourcePosition,
    /** Exclusive end position. */
    public val end: SourcePosition,
)

/**
 * Source file known to sqldelight-check.
 */
public data class SourceFile(
    /** Project-relative path used in diagnostics and reports. */
    public val path: String,
    /** Full file content at the time analysis starts. */
    public val content: String,
)

/**
 * Database family for known SQLDelight dialects.
 */
public enum class DialectFamily {
    /** SQLite dialect family, including SQLDelight's versioned SQLite dialect artifacts. */
    SQLite,

    /** MySQL dialect family. */
    MySql,

    /** PostgreSQL dialect family. */
    PostgreSql,

    /** HSQL dialect family. */
    Hsql,

    /** Third-party or otherwise unknown dialect family. */
    Custom,
}

/**
 * Metadata describing the SQL dialect used by a SQLDelight database.
 */
public data class SqlDialect(
    /** Broad dialect family used by rule applicability checks. */
    public val family: DialectFamily,
    /** Human-readable dialect name for reports. */
    public val displayName: String,
    /** Maven coordinate for the dialect artifact when known. */
    public val artifact: String? = null,
    /** Dialect artifact version when known. */
    public val version: String? = null,
    /** Implementation class name for custom dialects when available. */
    public val implementationClass: String? = null,
    /** Capability names discovered or inferred for this dialect. */
    public val capabilities: Set<String> = emptySet(),
)

/**
 * Lightweight SQLDelight database context visible to rules and reporters.
 */
public data class DatabaseContext(
    /** SQLDelight database name. */
    public val name: String,
    /** Dialect used to analyze files in this database. */
    public val dialect: SqlDialect,
)

/**
 * Text edit represented as a replacement over a source range.
 */
public data class TextEdit(
    /** Range to replace. */
    public val range: SourceRange,
    /** Replacement text. */
    public val replacement: String,
)

/**
 * A fix proposed by a rule diagnostic.
 */
public data class Fix(
    /** Short title shown in reports or IDE integrations. */
    public val title: String,
    /** Whether this fix may be applied by normal write tasks. */
    public val safety: FixSafety,
    /** Ordered edits that make up this fix. */
    public val edits: List<TextEdit>,
)

/**
 * Diagnostic emitted by a rule, adapter, formatter, or configuration validation.
 */
public data class Diagnostic(
    /** Rule ID responsible for the diagnostic when available. */
    public val ruleId: RuleId?,
    /** Resolved severity. */
    public val severity: Severity,
    /** User-facing message. */
    public val message: String,
    /** File where the diagnostic occurred. */
    public val file: SourceFile?,
    /** Source range where the diagnostic occurred. */
    public val range: SourceRange?,
    /** Database context associated with the diagnostic when known. */
    public val database: DatabaseContext?,
    /** Optional fixes for the diagnostic. */
    public val fixes: List<Fix> = emptyList(),
)

