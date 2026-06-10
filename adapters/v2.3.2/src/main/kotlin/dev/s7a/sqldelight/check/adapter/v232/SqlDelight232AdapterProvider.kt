package dev.s7a.sqldelight.check.adapter.v232

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.adapter.spi.AnalysisResult
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapter
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapterProvider
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.ServiceLoader

private const val COMPILATION_UNIT_CLASS = "app.cash.sqldelight.core.SqlDelightCompilationUnit"
private const val DATABASE_PROPERTIES_CLASS = "app.cash.sqldelight.core.SqlDelightDatabaseProperties"
private const val DIALECT_CLASS = "app.cash.sqldelight.dialect.api.SqlDelightDialect"
private const val ENVIRONMENT_CLASS = "app.cash.sqldelight.core.SqlDelightEnvironment"
private const val SOURCE_FOLDER_CLASS = "app.cash.sqldelight.core.SqlDelightSourceFolder"

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
        val packageName =
            input.packageName
                ?: return AnalysisResult(
                    files = input.files,
                    diagnostics = listOf(input.errorDiagnostic("SQLDelight package name was not resolved.")),
                )

        if (input.sourceFolders.isEmpty()) {
            return AnalysisResult(files = input.files, diagnostics = emptyList())
        }

        val diagnostics =
            runCatching {
                analyzeWithSqlDelight(
                    input = input,
                    packageName = packageName,
                )
            }.getOrElse { failure ->
                listOf(input.errorDiagnostic("SQLDelight 2.3.2 analysis failed: ${failure.message ?: failure::class.java.name}"))
            }
        return AnalysisResult(files = input.files, diagnostics = diagnostics)
    }

    private fun analyzeWithSqlDelight(
        input: AnalysisInput,
        packageName: String,
    ): List<Diagnostic> {
        val classpath = (input.compilerClasspath + input.dialectClasspath).distinctBy { file -> file.absolutePath }
        if (classpath.isEmpty()) {
            return listOf(input.errorDiagnostic("SQLDelight compiler classpath was not resolved."))
        }

        val outputDirectory = Files.createTempDirectory("sqldelight-check-${input.database.name}").toFile()
        try {
            return URLClassLoader(classpath.map { file -> file.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
                .use { loader -> analyzeWithClassLoader(input, packageName, outputDirectory, loader) }
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    private fun analyzeWithClassLoader(
        input: AnalysisInput,
        packageName: String,
        outputDirectory: File,
        loader: ClassLoader,
    ): List<Diagnostic> {
        val dialect = loader.loadDialect() ?: return listOf(input.errorDiagnostic("SQLDelight dialect implementation was not found."))
        val compilationUnit =
            loader.proxy(
                interfaceName = COMPILATION_UNIT_CLASS,
                values =
                    mapOf(
                        "getName" to input.database.name,
                        "getSourceFolders" to input.toSqlDelightSourceFolders(loader),
                        "getOutputDirectoryFile" to outputDirectory,
                    ),
            )
        val properties =
            loader.proxy(
                interfaceName = DATABASE_PROPERTIES_CLASS,
                values =
                    mapOf(
                        "getPackageName" to packageName,
                        "getCompilationUnits" to listOf(compilationUnit),
                        "getClassName" to input.database.name,
                        "getDependencies" to emptyList<Any>(),
                        "getDeriveSchemaFromMigrations" to false,
                        "getTreatNullAsUnknownForEquality" to false,
                        "getRootDirectory" to input.rootDirectory(),
                        "getGenerateAsync" to false,
                        "getExpandSelectStar" to true,
                    ),
            )
        val dialectClass = Class.forName(DIALECT_CLASS, true, loader)
        val environmentClass = Class.forName(ENVIRONMENT_CLASS, true, loader)
        val environment =
            environmentClass
                .constructors
                .first { constructor -> constructor.parameterCount == 5 }
                .newInstance(properties, compilationUnit, "sqldelight-check", false, dialectClass.cast(dialect))
        val status =
            environmentClass
                .methods
                .first { method -> method.name == "generateSqlDelightFiles" && method.parameterCount == 1 }
                .invoke(environment, { _: String -> Unit })

        return when {
            status.javaClass.name.endsWith("\$Failure") ->
                // FIXME: Map SQLDelight error text back to SourceFile and SourceRange before finalizing v0.1.0 reports.
                status.invokeNoArg("getErrors")
                    .asSequence()
                    .map { error -> input.errorDiagnostic(error.toString()) }
                    .toList()
            else -> emptyList()
        }
    }

    private fun AnalysisInput.toSqlDelightSourceFolders(loader: ClassLoader): Set<Any> {
        val local = sourceFolders.map { folder -> loader.sourceFolder(folder = folder, dependency = false) }
        val dependencies = dependencyFolders.map { folder -> loader.sourceFolder(folder = folder, dependency = true) }
        return (local + dependencies).toSet()
    }

    private fun ClassLoader.sourceFolder(
        folder: File,
        dependency: Boolean,
    ): Any =
        proxy(
            interfaceName = SOURCE_FOLDER_CLASS,
            values =
                mapOf(
                    "getFolder" to folder,
                    "getDependency" to dependency,
                ),
        )

    private fun AnalysisInput.rootDirectory(): File =
        sourceFolders
            .firstOrNull()
            ?.parentFile
            ?: dependencyFolders.firstOrNull()?.parentFile
            ?: File(".")

    private fun AnalysisInput.errorDiagnostic(message: String): Diagnostic =
        Diagnostic(
            ruleId = null,
            severity = Severity.Error,
            message = message,
            file = null,
            range = null,
            database = database,
        )
}

private fun ClassLoader.loadDialect(): Any? {
    val dialectClass = Class.forName(DIALECT_CLASS, true, this)
    return ServiceLoader.load(dialectClass, this).firstOrNull()
}

private fun ClassLoader.proxy(
    interfaceName: String,
    values: Map<String, Any?>,
): Any {
    val interfaceClass = Class.forName(interfaceName, true, this)
    return Proxy.newProxyInstance(this, arrayOf(interfaceClass)) { proxy, method, arguments ->
        when (method.name) {
            "equals" -> proxy === arguments?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "${interfaceClass.simpleName}${values}"
            else -> values[method.name]
        }
    }
}

private fun Any.invokeNoArg(name: String): Iterable<*> {
    val method: Method = javaClass.methods.first { candidate -> candidate.name == name && candidate.parameterCount == 0 }
    return method.invoke(this) as Iterable<*>
}
