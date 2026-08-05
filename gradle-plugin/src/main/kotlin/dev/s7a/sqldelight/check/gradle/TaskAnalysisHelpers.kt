package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.DialectRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Enumeration
import org.gradle.api.file.ConfigurableFileCollection

internal fun buildRuleRegistry(classpath: ConfigurableFileCollection): RuleRegistry =
    RuleRegistry.load(buildPluginClassLoader(classpath))

internal fun buildDialectRegistry(classpath: ConfigurableFileCollection): DialectRegistry =
    DialectRegistry.load(buildPluginClassLoader(classpath))

internal fun buildPluginClassLoader(classpath: ConfigurableFileCollection): ClassLoader {
    val urls = classpath.files.map { it.toURI().toURL() }.toTypedArray()
    return SqlDelightPluginClassLoader(urls, SqlDelightCheckGradlePlugin::class.java.classLoader)
}

internal fun buildAnalysisInputs(
    databases: List<SqlDelightDatabaseSpec>,
    reportRoot: String?,
    rootProjectDir: String,
    dialectRegistry: DialectRegistry,
): List<AnalysisInput> =
    databases.map { spec ->
        val coord = spec.dialectCoordinate.get()
        val dialect =
            if (coord.isEmpty()) {
                dialectRegistry.resolve(SqlDialectCoordinate(group = "", module = "", version = null))
            } else {
                val parts = coord.split(':')
                dialectRegistry.resolve(
                    SqlDialectCoordinate(
                        group = parts.getOrElse(0) { "" },
                        module = parts.getOrElse(1) { "" },
                        version = parts.getOrNull(2)?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
        AnalysisInput(
            database = DatabaseContext(name = spec.name.get(), dialect = dialect),
            files =
                spec.sourceFiles.files
                    .filter { it.isFile }
                    .sortedBy { it.path }
                    .distinctBy { it.path }
                    .map { file ->
                        SourceFile(
                            path = resolveReportPath(file, reportRoot, rootProjectDir),
                            content = file.readText(StandardCharsets.UTF_8),
                        )
                    },
        )
    }

internal fun resolveReportPath(
    file: File,
    reportRoot: String?,
    rootProjectDir: String,
): String {
    val filePath =
        file.toPath().toAbsolutePath().normalize().let {
            runCatching { it.toRealPath() }.getOrDefault(it)
        }
    val reportRootStr = reportRoot?.takeIf { it.isNotBlank() }
    if (reportRootStr != null) {
        val reportRootPath =
            File(reportRootStr).toPath().toAbsolutePath().normalize().let {
                runCatching { it.toRealPath() }.getOrDefault(it)
            }
        if (filePath.startsWith(reportRootPath)) {
            return reportRootPath.relativize(filePath).toString().replace(File.separatorChar, '/')
        }
    }
    val rootDir = File(rootProjectDir).toPath().toAbsolutePath().normalize()
    return rootDir.relativize(filePath).toString().replace(File.separatorChar, '/')
}

internal class SqlDelightPluginClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent) {
    override fun getResource(name: String): URL? = findResource(name) ?: parent.getResource(name)

    override fun getResources(name: String): Enumeration<URL> {
        val localResources = findResources(name).iterator().asSequence().toList()
        val parentResources = parent.getResources(name).iterator().asSequence().toList()
        return Collections.enumeration(localResources + parentResources)
    }
}
