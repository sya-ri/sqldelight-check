package dev.s7a.sqldelight.check.adapter.v232

import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapter
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapterProvider

/**
 * Adapter provider for SQLDelight 2.3.2.
 */
public class SqlDelight232AdapterProvider : SqlDelightAdapterProvider {
    override val id: String = "sqldelight-2.x"
    override val supportedVersions: Set<String> = setOf("2.0.x", "2.1.x", "2.2.x", "2.3.x", "2.4.x")

    override fun supports(version: String): Boolean = SqlDelight2VersionSupport.supports(version)

    override fun create(): SqlDelightAdapter = SqlDelight232Adapter
}
