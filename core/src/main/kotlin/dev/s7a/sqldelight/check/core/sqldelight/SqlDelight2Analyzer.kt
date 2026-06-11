package dev.s7a.sqldelight.check.core.sqldelight

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.AnalysisResult
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.ServiceLoader

/**
 * SQLDelight analyzer backed by SQLDelight 2.x runtime classes.
 */
internal object SqlDelight2Analyzer {
    fun analyze(input: AnalysisInput): AnalysisResult {
        if (input.sourceFolders.isEmpty()) {
            return AnalysisResult(files = input.files, diagnostics = emptyList())
        }

        val packageName =
            input.packageName
                ?: return AnalysisResult(
                    files = input.files,
                    diagnostics = listOf(input.errorDiagnostic("SQLDelight package name was not resolved.")),
                )

        val sqlDelightVersion = input.sqlDelightVersion
        if (sqlDelightVersion != null && !SqlDelight2VersionSupport.supports(sqlDelightVersion)) {
            return AnalysisResult(
                files = input.files,
                diagnostics = listOf(input.errorDiagnostic("SQLDelight $sqlDelightVersion is not supported by sqldelight-check 0.1.0.")),
            )
        }

        val diagnostics =
            runCatching {
                analyzeWithSqlDelight(
                    input = input,
                    packageName = packageName,
                )
            }.getOrElse { failure ->
                val rootCause = failure.rootCause()
                listOf(input.errorDiagnostic("SQLDelight 2.x analysis failed: ${rootCause.message ?: rootCause::class.java.name}"))
            }
        return AnalysisResult(files = input.files, diagnostics = diagnostics)
    }

    private fun analyzeWithSqlDelight(
        input: AnalysisInput,
        packageName: String,
    ): List<Diagnostic> {
        val classpath = (input.dialectClasspath + input.compilerClasspath).distinctBy { file -> file.absolutePath }
        if (classpath.isEmpty()) {
            return listOf(input.errorDiagnostic("SQLDelight dialect classpath was not resolved."))
        }

        val outputDirectory = Files.createTempDirectory("sqldelight-check-${input.database.name}").toFile()
        try {
            return URLClassLoader(
                classpath.map { file -> file.toURI().toURL() }.toTypedArray(),
                SqlDelightDialect::class.java.classLoader,
            )
                .use { loader ->
                    withContextClassLoader(loader) {
                        analyzeWithDialectClassLoader(input, packageName, outputDirectory, loader)
                    }
                }
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    private fun analyzeWithDialectClassLoader(
        input: AnalysisInput,
        packageName: String,
        outputDirectory: File,
        loader: ClassLoader,
    ): List<Diagnostic> {
        val dialect =
            ServiceLoader.load(SqlDelightDialect::class.java, loader).firstOrNull()
                ?: return listOf(input.errorDiagnostic("SQLDelight dialect implementation was not found."))
        val compilationUnit =
            SqlDelightCheckCompilationUnit(
                name = input.database.name,
                sourceFolders = input.toSqlDelightSourceFolders(),
                outputDirectoryFile = outputDirectory,
            )
        val properties =
            SqlDelightCheckDatabaseProperties(
                packageName = packageName,
                compilationUnits = listOf(compilationUnit),
                className = input.database.name,
                dependencies = emptyList(),
                deriveSchemaFromMigrations = false,
                treatNullAsUnknownForEquality = false,
                rootDirectory = input.rootDirectory(),
                generateAsync = false,
                expandSelectStar = true,
            )
        val environment =
            SqlDelightEnvironment(
                properties = properties,
                compilationUnit = compilationUnit,
                verifyMigrations = false,
                dialect = dialect,
                moduleName = "sqldelight-check",
                sourceFolders = input.sourceFolders,
                dependencyFolders = input.dependencyFolders,
            )
        val status = environment.generateSqlDelightFiles {}

        return when {
            status is SqlDelightEnvironment.CompilationStatus.Failure ->
                status.errors
                    .asSequence()
                    .map { error -> input.sqlDelightErrorDiagnostic(error) }
                    .toList()
            else -> emptyList()
        }
    }

    private fun AnalysisInput.toSqlDelightSourceFolders(): Set<SqlDelightSourceFolder> {
        val local = sourceFolders.map { folder -> SqlDelightCheckSourceFolder(folder = folder, dependency = false) }
        val dependencies = dependencyFolders.map { folder -> SqlDelightCheckSourceFolder(folder = folder, dependency = true) }
        return (local + dependencies).toSet()
    }

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

    private fun AnalysisInput.sqlDelightErrorDiagnostic(message: String): Diagnostic {
        val parsed = SqlDelightErrorMessage.parse(message) ?: return errorDiagnostic(message)
        val file = findSourceFile(parsed.path)
        return Diagnostic(
            ruleId = null,
            severity = Severity.Error,
            message = parsed.message,
            file = file,
            range = parsed.range,
            database = database,
        )
    }
}

private fun Throwable.rootCause(): Throwable {
    var current = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause ?: break
    }
    return current
}

private data class SqlDelightCheckCompilationUnit(
    override val name: String,
    override val sourceFolders: Set<SqlDelightSourceFolder>,
    override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit

private data class SqlDelightCheckDatabaseProperties(
    override val packageName: String,
    override val compilationUnits: List<SqlDelightCompilationUnit>,
    override val className: String,
    override val dependencies: List<SqlDelightDatabaseName>,
    override val deriveSchemaFromMigrations: Boolean,
    override val treatNullAsUnknownForEquality: Boolean,
    override val rootDirectory: File,
    override val generateAsync: Boolean,
    override val expandSelectStar: Boolean,
) : SqlDelightDatabaseProperties

private data class SqlDelightCheckSourceFolder(
    override val folder: File,
    override val dependency: Boolean,
) : SqlDelightSourceFolder

private inline fun <T> withContextClassLoader(
    classLoader: ClassLoader,
    block: () -> T,
): T {
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = classLoader
    return try {
        block()
    } finally {
        thread.contextClassLoader = previous
    }
}
