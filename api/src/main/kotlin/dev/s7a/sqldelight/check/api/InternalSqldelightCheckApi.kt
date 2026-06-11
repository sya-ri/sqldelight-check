package dev.s7a.sqldelight.check.api

/**
 * Marks API that is public only for sqldelight-check module boundaries.
 *
 * Custom rules and reporters should not opt in unless they intentionally depend
 * on implementation-level construction details.
 */
@RequiresOptIn(
    message = "This API is public for sqldelight-check internals and may change without source compatibility.",
    level = RequiresOptIn.Level.ERROR,
)
public annotation class InternalSqldelightCheckApi
