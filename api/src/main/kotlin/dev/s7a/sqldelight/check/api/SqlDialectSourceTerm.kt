package dev.s7a.sqldelight.check.api

/**
 * A SQL source term that source-text rules may need to recognize directly.
 */
public sealed interface SqlDialectSourceTerm {
    public val normalizedText: String

    /**
     * The `ALL` select modifier.
     */
    public data object All : SqlDialectSourceTerm {
        override val normalizedText: String = "all"
    }

    /**
     * The `AND` boolean operator.
     */
    public data object And : SqlDialectSourceTerm {
        override val normalizedText: String = "and"
    }

    /**
     * The `AS` alias or type mapping marker.
     */
    public data object As : SqlDialectSourceTerm {
        override val normalizedText: String = "as"
    }

    /**
     * The `ASC` ordering direction.
     */
    public data object Asc : SqlDialectSourceTerm {
        override val normalizedText: String = "asc"
    }

    /**
     * The `BETWEEN` range predicate.
     */
    public data object Between : SqlDialectSourceTerm {
        override val normalizedText: String = "between"
    }

    /**
     * The `BEGIN` transaction statement start.
     */
    public data object Begin : SqlDialectSourceTerm {
        override val normalizedText: String = "begin"
    }

    /**
     * The `BY` clause suffix term.
     */
    public data object By : SqlDialectSourceTerm {
        override val normalizedText: String = "by"
    }

    /**
     * The `CASE` expression start.
     */
    public data object Case : SqlDialectSourceTerm {
        override val normalizedText: String = "case"
    }

    /**
     * The `CHECK` constraint start.
     */
    public data object Check : SqlDialectSourceTerm {
        override val normalizedText: String = "check"
    }

    /**
     * The `COUNT` aggregate function name.
     */
    public data object Count : SqlDialectSourceTerm {
        override val normalizedText: String = "count"
    }

    /**
     * The `COLLATE` column constraint start.
     */
    public data object Collate : SqlDialectSourceTerm {
        override val normalizedText: String = "collate"
    }

    /**
     * The `COMMIT` transaction statement.
     */
    public data object Commit : SqlDialectSourceTerm {
        override val normalizedText: String = "commit"
    }

    /**
     * The `CONSTRAINT` named constraint marker.
     */
    public data object Constraint : SqlDialectSourceTerm {
        override val normalizedText: String = "constraint"
    }

    /**
     * The `CREATE` statement start.
     */
    public data object Create : SqlDialectSourceTerm {
        override val normalizedText: String = "create"
    }

    /**
     * The `CROSS` join modifier.
     */
    public data object Cross : SqlDialectSourceTerm {
        override val normalizedText: String = "cross"
    }

    /**
     * The `DEFAULT` column constraint start.
     */
    public data object Default : SqlDialectSourceTerm {
        override val normalizedText: String = "default"
    }

    /**
     * The `DELETE` statement start.
     */
    public data object Delete : SqlDialectSourceTerm {
        override val normalizedText: String = "delete"
    }

    /**
     * The `DESC` ordering direction.
     */
    public data object Desc : SqlDialectSourceTerm {
        override val normalizedText: String = "desc"
    }

    /**
     * The `DISTINCT` select modifier.
     */
    public data object Distinct : SqlDialectSourceTerm {
        override val normalizedText: String = "distinct"
    }

    /**
     * The `DO` upsert action introducer.
     */
    public data object Do : SqlDialectSourceTerm {
        override val normalizedText: String = "do"
    }

    /**
     * The `DROP` statement start.
     */
    public data object Drop : SqlDialectSourceTerm {
        override val normalizedText: String = "drop"
    }

    /**
     * The `ELSE` case branch marker.
     */
    public data object Else : SqlDialectSourceTerm {
        override val normalizedText: String = "else"
    }

    /**
     * The `END` case expression terminator.
     */
    public data object End : SqlDialectSourceTerm {
        override val normalizedText: String = "end"
    }

    /**
     * The `ESCAPE` LIKE predicate clause.
     */
    public data object Escape : SqlDialectSourceTerm {
        override val normalizedText: String = "escape"
    }

    /**
     * The `EXCEPT` set operator.
     */
    public data object Except : SqlDialectSourceTerm {
        override val normalizedText: String = "except"
    }

    /**
     * The `EXISTS` predicate.
     */
    public data object Exists : SqlDialectSourceTerm {
        override val normalizedText: String = "exists"
    }

    /**
     * The `FALSE` literal.
     */
    public data object False : SqlDialectSourceTerm {
        override val normalizedText: String = "false"
    }

    /**
     * The `FETCH` row limiting clause.
     */
    public data object Fetch : SqlDialectSourceTerm {
        override val normalizedText: String = "fetch"
    }

    /**
     * The `FILTER` aggregate clause.
     */
    public data object Filter : SqlDialectSourceTerm {
        override val normalizedText: String = "filter"
    }

    /**
     * The `FIRST` null placement or fetch direction term.
     */
    public data object First : SqlDialectSourceTerm {
        override val normalizedText: String = "first"
    }

    /**
     * The `FOREIGN` table constraint prefix.
     */
    public data object Foreign : SqlDialectSourceTerm {
        override val normalizedText: String = "foreign"
    }

    /**
     * The `FULL` join modifier.
     */
    public data object Full : SqlDialectSourceTerm {
        override val normalizedText: String = "full"
    }

    /**
     * The `GENERATED` column constraint start.
     */
    public data object Generated : SqlDialectSourceTerm {
        override val normalizedText: String = "generated"
    }

    /**
     * The `FROM` clause start.
     */
    public data object From : SqlDialectSourceTerm {
        override val normalizedText: String = "from"
    }

    /**
     * The `GROUP` clause start.
     */
    public data object Group : SqlDialectSourceTerm {
        override val normalizedText: String = "group"
    }

    /**
     * The `HAVING` clause start.
     */
    public data object Having : SqlDialectSourceTerm {
        override val normalizedText: String = "having"
    }

    /**
     * The `IN` predicate.
     */
    public data object In : SqlDialectSourceTerm {
        override val normalizedText: String = "in"
    }

    /**
     * The `INNER` join modifier.
     */
    public data object Inner : SqlDialectSourceTerm {
        override val normalizedText: String = "inner"
    }

    /**
     * The `INSERT` statement start.
     */
    public data object Insert : SqlDialectSourceTerm {
        override val normalizedText: String = "insert"
    }

    /**
     * The `INTERSECT` set operator.
     */
    public data object Intersect : SqlDialectSourceTerm {
        override val normalizedText: String = "intersect"
    }

    /**
     * The `INTO` clause marker.
     */
    public data object Into : SqlDialectSourceTerm {
        override val normalizedText: String = "into"
    }

    /**
     * The `IS` comparison operator.
     */
    public data object Is : SqlDialectSourceTerm {
        override val normalizedText: String = "is"
    }

    /**
     * The `JOIN` clause marker.
     */
    public data object Join : SqlDialectSourceTerm {
        override val normalizedText: String = "join"
    }

    /**
     * The `LAST` null placement term.
     */
    public data object Last : SqlDialectSourceTerm {
        override val normalizedText: String = "last"
    }

    /**
     * The `LEFT` join modifier.
     */
    public data object Left : SqlDialectSourceTerm {
        override val normalizedText: String = "left"
    }

    /**
     * The `LIKE` predicate.
     */
    public data object Like : SqlDialectSourceTerm {
        override val normalizedText: String = "like"
    }

    /**
     * The `LIMIT` clause start.
     */
    public data object Limit : SqlDialectSourceTerm {
        override val normalizedText: String = "limit"
    }

    /**
     * The `NOT` predicate or constraint modifier.
     */
    public data object Not : SqlDialectSourceTerm {
        override val normalizedText: String = "not"
    }

    /**
     * The `NATURAL` join modifier.
     */
    public data object Natural : SqlDialectSourceTerm {
        override val normalizedText: String = "natural"
    }

    /**
     * The `NULL` literal or constraint marker.
     */
    public data object Null : SqlDialectSourceTerm {
        override val normalizedText: String = "null"
    }

    /**
     * The `NULLS` ordering modifier.
     */
    public data object Nulls : SqlDialectSourceTerm {
        override val normalizedText: String = "nulls"
    }

    /**
     * The `OFFSET` clause start.
     */
    public data object Offset : SqlDialectSourceTerm {
        override val normalizedText: String = "offset"
    }

    /**
     * The `ON` join condition marker.
     */
    public data object On : SqlDialectSourceTerm {
        override val normalizedText: String = "on"
    }

    /**
     * The `OR` boolean operator.
     */
    public data object Or : SqlDialectSourceTerm {
        override val normalizedText: String = "or"
    }

    /**
     * The `ORDER` clause start.
     */
    public data object Order : SqlDialectSourceTerm {
        override val normalizedText: String = "order"
    }

    /**
     * The `OVER` window clause marker.
     */
    public data object Over : SqlDialectSourceTerm {
        override val normalizedText: String = "over"
    }

    /**
     * The `OUTER` join modifier.
     */
    public data object Outer : SqlDialectSourceTerm {
        override val normalizedText: String = "outer"
    }

    /**
     * The `PRIMARY` constraint prefix.
     */
    public data object Primary : SqlDialectSourceTerm {
        override val normalizedText: String = "primary"
    }

    /**
     * The `REFERENCES` column constraint start.
     */
    public data object References : SqlDialectSourceTerm {
        override val normalizedText: String = "references"
    }

    /**
     * The `RIGHT` join modifier.
     */
    public data object Right : SqlDialectSourceTerm {
        override val normalizedText: String = "right"
    }

    /**
     * The `ROLLBACK` transaction statement.
     */
    public data object Rollback : SqlDialectSourceTerm {
        override val normalizedText: String = "rollback"
    }

    /**
     * The `SELECT` statement start.
     */
    public data object Select : SqlDialectSourceTerm {
        override val normalizedText: String = "select"
    }

    /**
     * The `SET` clause start.
     */
    public data object Set : SqlDialectSourceTerm {
        override val normalizedText: String = "set"
    }

    /**
     * The `TABLE` object kind.
     */
    public data object Table : SqlDialectSourceTerm {
        override val normalizedText: String = "table"
    }

    /**
     * The `THEN` case branch marker.
     */
    public data object Then : SqlDialectSourceTerm {
        override val normalizedText: String = "then"
    }

    /**
     * The `TRANSACTION` statement term.
     */
    public data object Transaction : SqlDialectSourceTerm {
        override val normalizedText: String = "transaction"
    }

    /**
     * The `TRIGGER` object kind.
     */
    public data object Trigger : SqlDialectSourceTerm {
        override val normalizedText: String = "trigger"
    }

    /**
     * The `TRUE` literal.
     */
    public data object True : SqlDialectSourceTerm {
        override val normalizedText: String = "true"
    }

    /**
     * The `UNION` set operator.
     */
    public data object Union : SqlDialectSourceTerm {
        override val normalizedText: String = "union"
    }

    /**
     * The `UNIQUE` constraint start.
     */
    public data object Unique : SqlDialectSourceTerm {
        override val normalizedText: String = "unique"
    }

    /**
     * The `UPDATE` statement start.
     */
    public data object Update : SqlDialectSourceTerm {
        override val normalizedText: String = "update"
    }

    /**
     * The `USING` join condition marker.
     */
    public data object Using : SqlDialectSourceTerm {
        override val normalizedText: String = "using"
    }

    /**
     * The `VALUES` insert clause.
     */
    public data object Values : SqlDialectSourceTerm {
        override val normalizedText: String = "values"
    }

    /**
     * The `VIEW` object kind.
     */
    public data object View : SqlDialectSourceTerm {
        override val normalizedText: String = "view"
    }

    /**
     * The `WHEN` case branch marker.
     */
    public data object When : SqlDialectSourceTerm {
        override val normalizedText: String = "when"
    }

    /**
     * The `WHERE` clause start.
     */
    public data object Where : SqlDialectSourceTerm {
        override val normalizedText: String = "where"
    }

    /**
     * The `WINDOW` clause start.
     */
    public data object Window : SqlDialectSourceTerm {
        override val normalizedText: String = "window"
    }

    /**
     * The `WITH` common table expression or statement prefix.
     */
    public data object With : SqlDialectSourceTerm {
        override val normalizedText: String = "with"
    }
}
