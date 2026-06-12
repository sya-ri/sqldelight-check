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
     * Starts an `ALTER TABLE` statement.
     */
    public data object AlterTableStatementStart : SqlDialectSourcePatternRole

    /**
     * Drops a column from an existing table.
     */
    public data object ColumnDropOperation : SqlDialectSourcePatternRole

    /**
     * Renames a column in an existing table.
     */
    public data object ColumnRenameOperation : SqlDialectSourcePatternRole

    /**
     * Renames an existing table.
     */
    public data object TableRenameOperation : SqlDialectSourcePatternRole

    /**
     * Adds a column to an existing table.
     */
    public data object ColumnAddOperation : SqlDialectSourcePatternRole

    /**
     * Modifies an existing column definition.
     */
    public data object ColumnModifyOperation : SqlDialectSourcePatternRole

    /**
     * Changes an existing column definition.
     */
    public data object ColumnChangeOperation : SqlDialectSourcePatternRole

    /**
     * Alters a column in an existing table.
     */
    public data object ColumnAlterOperation : SqlDialectSourcePatternRole

    /**
     * Changes an existing column type.
     */
    public data object ColumnTypeChangeOperation : SqlDialectSourcePatternRole

    /**
     * Changes an existing column to `NOT NULL`.
     */
    public data object ColumnSetNotNullOperation : SqlDialectSourcePatternRole

    /**
     * Adds a table constraint.
     */
    public data object ConstraintAddOperation : SqlDialectSourcePatternRole

    /**
     * Marks a `NOT VALID` constraint clause.
     */
    public data object NotValidConstraintClause : SqlDialectSourcePatternRole

    /**
     * Starts a `CREATE INDEX` statement.
     */
    public data object CreateIndexStatementStart : SqlDialectSourcePatternRole

    /**
     * Starts a `CREATE TABLE` statement.
     */
    public data object CreateTableStatementStart : SqlDialectSourcePatternRole

    /**
     * Starts a concurrent `CREATE INDEX` statement.
     */
    public data object CreateConcurrentIndexStatementStart : SqlDialectSourcePatternRole

    /**
     * Marks a `CONCURRENTLY` clause.
     */
    public data object ConcurrentlyClause : SqlDialectSourcePatternRole

    /**
     * Starts a `REINDEX` statement.
     */
    public data object ReindexStatementStart : SqlDialectSourcePatternRole

    /**
     * Marks a `REINDEX SYSTEM` target.
     */
    public data object ReindexSystemTarget : SqlDialectSourcePatternRole

    /**
     * Starts an explicit transaction.
     */
    public data object TransactionStartStatement : SqlDialectSourcePatternRole

    /**
     * Ends an explicit transaction.
     */
    public data object TransactionEndStatement : SqlDialectSourcePatternRole

    /**
     * Marks a volatile default function.
     */
    public data object VolatileDefaultFunction : SqlDialectSourcePatternRole

    /**
     * Starts a default-value clause.
     */
    public data object DefaultValueClause : SqlDialectSourcePatternRole

    /**
     * Marks a serial-style generated integer type.
     */
    public data object SerialDataTypeName : SqlDialectSourcePatternRole

    /**
     * Marks a MySQL `COPY` alter-table algorithm clause.
     */
    public data object CopyAlgorithmClause : SqlDialectSourcePatternRole

    /**
     * Marks a MySQL exclusive lock clause.
     */
    public data object ExclusiveLockClause : SqlDialectSourcePatternRole

    /**
     * Marks a legacy MySQL `utf8` character set declaration.
     */
    public data object LegacyUtf8CharsetDeclaration : SqlDialectSourcePatternRole

    /**
     * Starts a MySQL `REPLACE INTO` statement.
     */
    public data object ReplaceIntoStatementStart : SqlDialectSourcePatternRole

    /**
     * Marks an integer type that can carry a display width.
     */
    public data object IntegerDisplayWidthType : SqlDialectSourcePatternRole

    /**
     * Marks SQLite `AUTOINCREMENT`.
     */
    public data object AutoincrementKeyword : SqlDialectSourcePatternRole

    /**
     * Starts a SQLite `INSERT OR REPLACE` statement.
     */
    public data object InsertOrReplaceStatementStart : SqlDialectSourcePatternRole

    /**
     * Starts a SQLite `ON CONFLICT` clause.
     */
    public data object OnConflictClause : SqlDialectSourcePatternRole

    /**
     * Marks a SQLite `DO UPDATE` conflict action.
     */
    public data object DoUpdateClause : SqlDialectSourcePatternRole

    /**
     * Starts a SQLite foreign-key pragma statement.
     */
    public data object ForeignKeysPragmaStatementStart : SqlDialectSourcePatternRole

    /**
     * Marks a SQLite foreign-key pragma value that disables enforcement.
     */
    public data object ForeignKeysOffValue : SqlDialectSourcePatternRole

    /**
     * Marks a SQLite foreign-key pragma value that enables enforcement.
     */
    public data object ForeignKeysOnValue : SqlDialectSourcePatternRole

    /**
     * Marks an alter-table operation that SQLite cannot apply in place.
     */
    public data object ComplexAlterTableOperation : SqlDialectSourcePatternRole

    /**
     * Marks a SQLite rowid-primary-key type that is not exact `INTEGER`.
     */
    public data object NonIntegerRowidPrimaryKeyType : SqlDialectSourcePatternRole

    /**
     * Marks a primary-key constraint.
     */
    public data object PrimaryKeyConstraint : SqlDialectSourcePatternRole

    /**
     * Marks a SQLite `WITHOUT ROWID` clause.
     */
    public data object WithoutRowidClause : SqlDialectSourcePatternRole

    /**
     * Marks an HSQL database or file setting statement.
     */
    public data object DatabaseFileSettingStatement : SqlDialectSourcePatternRole

    /**
     * Marks an HSQL system operation statement.
     */
    public data object SystemOperationStatement : SqlDialectSourcePatternRole

    /**
     * Marks an HSQL text-table source statement.
     */
    public data object TextTableSourceStatement : SqlDialectSourcePatternRole

    /**
     * Marks an HSQL text-table source clause.
     */
    public data object TextTableSourceClause : SqlDialectSourcePatternRole

    /**
     * Starts an HSQL text-table source binding statement.
     */
    public data object TextTableSourceBindingStart : SqlDialectSourcePatternRole

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
