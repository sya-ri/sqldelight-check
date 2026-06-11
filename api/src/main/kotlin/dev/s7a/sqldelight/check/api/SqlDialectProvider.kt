package dev.s7a.sqldelight.check.api

/**
 * Resolves rule-relevant dialect metadata for a SQLDelight dialect artifact.
 *
 * Implementations can be published on the sqldelight-check dialect provider
 * classpath and discovered with Java `ServiceLoader`.
 */
public fun interface SqlDialectProvider {
    /**
     * Returns dialect metadata for [coordinate], or `null` when this provider
     * does not handle the artifact.
     */
    public fun resolve(coordinate: SqlDialectCoordinate): SqlDialect?
}
