package dev.s7a.sqldelight.check.api

/**
 * The source-scanner meaning attached to a dialect pattern.
 */
public interface SqlDialectSourcePatternRole {
    /**
     * Marks a term that cannot be a result alias.
     */
    public data object AliasBoundary : SqlDialectSourcePatternRole

    /**
     * Ends a table reference segment.
     */
    public data object TableReferenceBoundary : SqlDialectSourcePatternRole

    /**
     * Marks a join type modifier.
     */
    public data object JoinModifier : SqlDialectSourcePatternRole

    /**
     * Starts a SQL statement.
     */
    public data object StatementStart : SqlDialectSourcePatternRole

    /**
     * Starts a SQLDelight statement or declaration body.
     */
    public data object SqlDelightStatementStart : SqlDialectSourcePatternRole

    /**
     * Starts a SQLDelight executable query statement.
     */
    public data object SqlDelightExecutableStatementStart : SqlDialectSourcePatternRole

    /**
     * Continues a statement after a SQLDelight prefix.
     */
    public data object StatementContinuation : SqlDialectSourcePatternRole

    /**
     * Starts a select target list.
     */
    public data object SelectListStart : SqlDialectSourcePatternRole

    /**
     * Starts or ends a major SQL clause.
     */
    public data object ClauseBoundary : SqlDialectSourcePatternRole

    /**
     * Starts a major clause that should appear on its own line.
     */
    public data object MajorClauseStart : SqlDialectSourcePatternRole

    /**
     * Starts a predicate clause.
     */
    public data object PredicateStart : SqlDialectSourcePatternRole

    /**
     * Ends a predicate clause.
     */
    public data object PredicateBoundary : SqlDialectSourcePatternRole

    /**
     * Ends a join condition segment.
     */
    public data object JoinConditionBoundary : SqlDialectSourcePatternRole

    /**
     * Marks a boolean operator.
     */
    public data object BooleanOperator : SqlDialectSourcePatternRole

    /**
     * Marks a set operator.
     */
    public data object SetOperator : SqlDialectSourcePatternRole

    /**
     * Starts a column constraint.
     */
    public data object ColumnConstraintStart : SqlDialectSourcePatternRole

    /**
     * Starts a table constraint.
     */
    public data object TableConstraintStart : SqlDialectSourcePatternRole

    /**
     * Ends a `GROUP BY` target list.
     */
    public data object GroupByBoundary : SqlDialectSourcePatternRole

    /**
     * Ends an `ORDER BY` target list.
     */
    public data object OrderByBoundary : SqlDialectSourcePatternRole

    /**
     * Marks a token checked by keyword casing rules.
     */
    public data object KeywordCaseTarget : SqlDialectSourcePatternRole

    /**
     * Marks a common SQL function name.
     */
    public data object CommonFunctionName : SqlDialectSourcePatternRole

    /**
     * Marks a function that should be replaced with `COALESCE`.
     */
    public data object CoalesceAlternativeFunction : SqlDialectSourcePatternRole

    /**
     * Marks a function that can make index usage harder in predicates.
     */
    public data object IndexUnfriendlyFunction : SqlDialectSourcePatternRole

    /**
     * Marks a SQL data type name.
     */
    public data object DataTypeName : SqlDialectSourcePatternRole

    /**
     * Marks a SQLDelight storage type that can be mapped to a Kotlin type.
     */
    public data object SqlDelightMappableStorageTypeName : SqlDialectSourcePatternRole

    /**
     * Continues an expression at the same syntactic level.
     */
    public data object ExpressionContinuation : SqlDialectSourcePatternRole

    /**
     * Continues an expression inside parentheses.
     */
    public data object ParenthesizedExpressionContinuation : SqlDialectSourcePatternRole
}
