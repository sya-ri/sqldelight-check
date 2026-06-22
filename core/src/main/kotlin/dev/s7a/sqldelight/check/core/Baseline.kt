package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId

/**
 * Diagnostics that should be ignored because they are already known.
 */
public class Baseline(
    public val entries: Set<BaselineEntry> = emptySet(),
) {
    public fun suppresses(diagnostic: Diagnostic): Boolean =
        BaselineEntry.from(diagnostic)?.let(entries::contains) == true

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Baseline &&
            entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "Baseline(entries=$entries)"

    public companion object {
        public val Empty: Baseline = Baseline()
    }
}

/**
 * One known diagnostic in a sqldelight-check baseline file.
 */
public class BaselineEntry(
    public val database: String,
    public val ruleId: QualifiedRuleId,
    public val path: String,
    public val line: Int,
    public val column: Int,
    public val message: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is BaselineEntry &&
            database == other.database &&
            ruleId == other.ruleId &&
            path == other.path &&
            line == other.line &&
            column == other.column &&
            message == other.message

    override fun hashCode(): Int {
        var result = database.hashCode()
        result = 31 * result + ruleId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + line
        result = 31 * result + column
        result = 31 * result + message.hashCode()
        return result
    }

    override fun toString(): String =
        "BaselineEntry(database=$database, ruleId=$ruleId, path=$path, line=$line, column=$column, message=$message)"

    public companion object {
        public fun from(diagnostic: Diagnostic): BaselineEntry? {
            val file = diagnostic.file ?: return null
            val position = diagnostic.range?.start ?: return null
            val database = diagnostic.database ?: return null
            return BaselineEntry(
                database = database.name,
                ruleId = diagnostic.ruleId,
                path = file.path,
                line = position.line,
                column = position.column,
                message = diagnostic.message,
            )
        }
    }
}
