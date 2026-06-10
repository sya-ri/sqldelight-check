package dev.s7a.sqldelight.check.adapter.v232

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.adapter.spi.AnalysisResult
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapter
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapterProvider

/**
 * Adapter provider for SQLDelight 2.3.2.
 */
public class SqlDelight232AdapterProvider : SqlDelightAdapterProvider {
    override val id: String = "sqldelight-2.3.2"
    override val supportedVersions: Set<String> = setOf("2.3.2")

    override fun create(): SqlDelightAdapter = SqlDelight232Adapter
}

private object SqlDelight232Adapter : SqlDelightAdapter {
    override fun analyze(input: AnalysisInput): AnalysisResult {
        // FIXME: Integrate SQLDelight 2.3.2 compiler/parser APIs and official dialect handling.
        return AnalysisResult(files = input.files, diagnostics = emptyList())
    }
}

