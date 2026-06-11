package dev.s7a.sqldelight.check.api

/**
 * A fix proposed by a rule diagnostic.
 */
public class Fix(
    /**
     * Short title shown in reports or IDE integrations.
     */
    public val title: String,
    /**
     * Whether this fix may be applied by normal write tasks.
     */
    public val safety: FixSafety,
    /**
     * Ordered edits that make up this fix.
     */
    public val edits: List<TextEdit>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Fix &&
            title == other.title &&
            safety == other.safety &&
            edits == other.edits

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + safety.hashCode()
        result = 31 * result + edits.hashCode()
        return result
    }

    override fun toString(): String = "Fix(title=$title, safety=$safety, edits=$edits)"
}
