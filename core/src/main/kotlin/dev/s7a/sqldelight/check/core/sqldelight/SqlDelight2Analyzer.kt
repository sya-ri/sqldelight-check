package dev.s7a.sqldelight.check.core.sqldelight

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.AnalysisResult
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.Files

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
                listOf(input.errorDiagnostic("SQLDelight 2.x analysis failed: ${failure.message ?: failure::class.java.name}"))
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
                interfaceName = SQL_DELIGHT_COMPILATION_UNIT_CLASS,
                values =
                    mapOf(
                        "getName" to input.database.name,
                        "getSourceFolders" to input.toSqlDelightSourceFolders(loader),
                        "getOutputDirectoryFile" to outputDirectory,
                    ),
            )
        val properties =
            loader.proxy(
                interfaceName = SQL_DELIGHT_DATABASE_PROPERTIES_CLASS,
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
        val dialectClass = Class.forName(SQL_DELIGHT_DIALECT_CLASS, true, loader)
        val environmentClass = Class.forName(SQL_DELIGHT_ENVIRONMENT_CLASS, true, loader)
        val environment = environmentClass.newEnvironment(properties, compilationUnit, dialectClass.cast(dialect), input)
        val status =
            environmentClass
                .methodWithParameterCount("generateSqlDelightFiles", 1)
                .invoke(environment, { _: String -> Unit })

        return when {
            status.javaClass.name.endsWith("\$Failure") ->
                status.invokeNoArg("getErrors")
                    .asSequence()
                    .map { error -> input.sqlDelightErrorDiagnostic(error.toString()) }
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
            interfaceName = SQL_DELIGHT_SOURCE_FOLDER_CLASS,
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

    private fun Class<*>.newEnvironment(
        properties: Any,
        compilationUnit: Any,
        dialect: Any,
        input: AnalysisInput,
    ): Any {
        val constructor =
            constructors.firstOrNull { candidate -> candidate.parameterCount == 7 }
                ?: constructors.firstOrNull { candidate -> candidate.parameterCount == 8 }
                ?: error(
                    "SQLDelight class $name does not expose a supported constructor. " +
                        "Available constructors: ${constructors.joinToString { constructor -> constructor.signature() }}",
                )
        val arguments =
            when (constructor.parameterCount) {
                7 ->
                    arrayOf(
                        properties,
                        compilationUnit,
                        false,
                        dialect,
                        "sqldelight-check",
                        input.sourceFolders,
                        input.dependencyFolders,
                    )
                8 ->
                    arrayOf(
                        properties,
                        compilationUnit,
                        false,
                        dialect,
                        "sqldelight-check",
                        emptySet<Any>(),
                        input.sourceFolders,
                        input.dependencyFolders,
                    )
                else -> error("Unsupported SQLDelightEnvironment constructor: ${constructor.signature()}")
            }
        return constructor.newInstance(*arguments)
    }

    private fun Class<*>.methodWithParameterCount(
        name: String,
        parameterCount: Int,
    ): Method =
        methods.firstOrNull { method -> method.name == name && method.parameterCount == parameterCount }
            ?: error(
                "SQLDelight class ${this.name} does not expose $name with $parameterCount argument(s). " +
                    "Available methods: ${methods.joinToString { method -> method.signature() }}",
            )

    private fun Constructor<*>.signature(): String =
        parameterTypes.joinToString(prefix = "(", postfix = ")") { type -> type.name }

    private fun Method.signature(): String =
        "$name${parameterTypes.joinToString(prefix = "(", postfix = ")") { type -> type.name }}"
}
