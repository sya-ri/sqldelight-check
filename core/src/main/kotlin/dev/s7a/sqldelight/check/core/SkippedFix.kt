package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * A diagnostic fix that was not applied.
 */
public class SkippedFix(
    /**
     * Rule ID that produced the fix.
     */
    public val ruleId: QualifiedRuleId,
    /**
     * File that contained the skipped fix when available.
     */
    public val file: SourceFile?,
    /**
     * User-facing fix title.
     */
    public val title: String,
    /**
     * Reason the fix was not applied.
     */
    public val reason: FixSkipReason,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SkippedFix &&
            ruleId == other.ruleId &&
            file == other.file &&
            title == other.title &&
            reason == other.reason

    override fun hashCode(): Int {
        var result = ruleId.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + reason.hashCode()
        return result
    }

    override fun toString(): String = "SkippedFix(ruleId=$ruleId, file=$file, title=$title, reason=$reason)"
}
