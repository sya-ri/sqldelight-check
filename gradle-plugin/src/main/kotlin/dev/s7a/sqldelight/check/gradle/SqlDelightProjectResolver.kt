package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import java.io.File
import java.nio.charset.StandardCharsets
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection

private const val SQLDELIGHT_TASK_CLASS_NAME = "app.cash.sqldelight.gradle.SqlDelightTask"
private const val DEFAULT_SQLDELIGHT_VERSION = "2.3.2"
private val SQLDELIGHT_COMPILER_CLASSES =
    listOf(
        "app.cash.sqldelight.core.SqlDelightCompilationUnit",
        "app.cash.sqldelight.core.SqlDelightDatabaseProperties",
        "app.cash.sqldelight.core.SqlDelightSourceFolder",
        "app.cash.sqldelight.dialect.api.SqlDelightDialect",
        "com.alecstrong.sql.psi.core.SqlCoreEnvironment",
    )

/**
 * SQLDelight database input resolved from a Gradle project.
 */
internal data class ResolvedSqlDelightInput(
    /**
     * SQLDelight version used for this database.
     */
    val sqlDelightVersion: String,
    /**
     * Core analysis input for sqldelight-check.
     */
    val analysisInput: AnalysisInput,
)

/**
 * Reads SQLDelight's generated Gradle task model without linking sqldelight-check to SQLDelight Gradle classes.
 */
internal class SqlDelightProjectResolver(
    private val project: Project,
) {
    /**
     * Resolves SQLDelight database inputs known to the project.
     */
    fun resolve(): List<ResolvedSqlDelightInput> {
        val resolved =
            project.tasks
                .filter { task -> task.hasSuperclass(SQLDELIGHT_TASK_CLASS_NAME) }
                .mapNotNull { task -> resolveTask(task) }

        return resolved
            .groupBy { input -> input.analysisInput.database.name }
            .map { (_, inputs) -> mergeInputs(inputs) }
    }

    private fun resolveTask(task: Any): ResolvedSqlDelightInput? {
        val properties = task.invokeNoArg("getProperties").unwrapGradleProvider()
        val compilationUnit = task.compilationUnit(properties)
        val databaseName = properties.invokeNoArg("getClassName") as String
        val packageName = properties.invokeNoArg("getPackageName") as String
        val dialectConfiguration = project.configurations.findByName("${databaseName}DialectClasspath")
        val intellijConfiguration = project.configurations.findByName("${databaseName}IntellijEnv")
        val dialect = resolveDialect(dialectConfiguration)
        val version = resolveSqlDelightVersion(intellijConfiguration, dialect).orElse(DEFAULT_SQLDELIGHT_VERSION)
        val sourceFolders = resolveSourceFolders(compilationUnit)
        val localSourceFolders = sourceFolders.filterNot { folder -> folder.dependency }
        val dependencySourceFolders = sourceFolders.filter { folder -> folder.dependency }
        val sourceFiles = resolveSourceFiles(localSourceFolders)
        val dialectClasspath = dialectConfiguration?.files?.toList().orEmpty()
        val intellijClasspath = intellijConfiguration?.files?.toList().orEmpty()
        val compilerClasspath = task.sqlDelightCompilerClasspath() + dialectClasspath + intellijClasspath

        return ResolvedSqlDelightInput(
            sqlDelightVersion = version,
            analysisInput =
                AnalysisInput(
                    database = DatabaseContext(name = databaseName, dialect = dialect),
                    files = sourceFiles,
                    sqlDelightVersion = version,
                    packageName = packageName,
                    sourceFolders = localSourceFolders.map { folder -> folder.file },
                    dependencyFolders = dependencySourceFolders.map { folder -> folder.file },
                    compilerClasspath = compilerClasspath.distinctBy { file -> file.absolutePath },
                    dialectClasspath = dialectClasspath,
                ),
        )
    }

    private fun mergeInputs(inputs: List<ResolvedSqlDelightInput>): ResolvedSqlDelightInput {
        val first = inputs.first()
        val files =
            inputs
                .flatMap { input -> input.analysisInput.files }
                .distinctBy { file -> file.path }
                .sortedBy { file -> file.path }
        return first.copy(
            analysisInput = first.analysisInput.copy(files = files),
        )
    }

    private fun resolveSourceFolders(compilationUnit: Any): List<ResolvedSourceFolder> {
        val sourceFolders = compilationUnit.invokeNoArg("getSourceFolders") as Iterable<*>
        return sourceFolders
            .mapNotNull { sourceFolder ->
                sourceFolder ?: return@mapNotNull null
                val file = sourceFolder.invokeNoArg("getFolder") as? File ?: return@mapNotNull null
                val dependency = sourceFolder.invokeNoArg("getDependency") as Boolean
                ResolvedSourceFolder(file = file, dependency = dependency)
            }
    }

    private fun resolveSourceFiles(sourceFolders: List<ResolvedSourceFolder>): List<SourceFile> =
        sourceFolders
            .map { sourceFolder -> sourceFolder.file }
            .flatMap { folder -> sqlDelightFiles(folder) }
            .distinctBy { file -> file.path }
            .sortedBy { file -> file.path }

    private fun sqlDelightFiles(folder: File): List<SourceFile> {
        if (!folder.exists()) return emptyList()
        return project.fileTree(folder) { tree ->
            tree.include("**/*.sq")
            tree.include("**/*.sqm")
        }.files.map { file ->
            SourceFile(
                path = project.relativePath(file),
                content = file.readText(StandardCharsets.UTF_8),
            )
        }
    }

    private fun Any.compilationUnit(properties: Any): Any =
        invokeNoArgOrNull("getCompilationUnit")
            ?.unwrapGradleProvider()
            ?: properties
                .invokeNoArg("getCompilationUnits")
                .unwrapGradleProvider()
                .asIterable()
                .firstOrNull()
            ?: error("SQLDelight database properties ${properties.javaClass.name} do not expose a compilation unit.")

    private fun resolveSqlDelightVersion(
        configuration: Configuration?,
        dialect: SqlDialect,
    ): String? {
        val compilerEnvVersion =
            configuration
                ?.moduleComponents()
                ?.firstOrNull { component ->
                    component.isSqlDelightModule(SqlDelightModule.CompilerEnv.moduleName)
                }
                ?.version
        return compilerEnvVersion ?: dialect.version
    }

    private fun resolveDialect(configuration: Configuration?): SqlDialect {
        val directDialect = configuration?.directModuleDependencies()?.firstOrNull()
        if (directDialect != null) {
            return sqlDialectFromCoordinate(
                group = directDialect.group.orEmpty(),
                module = directDialect.name,
                version = directDialect.version,
            )
        }

        val resolvedDialect =
            configuration
                ?.moduleComponents()
                ?.firstOrNull { component -> component.isDialectArtifact() }
        if (resolvedDialect != null) {
            return sqlDialectFromCoordinate(
                group = resolvedDialect.group,
                module = resolvedDialect.module,
                version = resolvedDialect.version,
            )
        }

        return SqlDialect(
            family = DialectFamily.Custom,
            displayName = "Custom SQLDelight dialect",
        )
    }

}

private data class ResolvedSourceFolder(
    val file: File,
    val dependency: Boolean,
)

private enum class SqlDelightModule(
    val moduleName: String,
) {
    CompilerEnv("compiler-env"),
}

private fun Any.hasSuperclass(className: String): Boolean {
    var current: Class<*>? = javaClass
    while (current != null) {
        if (current.name == className) return true
        current = current.superclass
    }
    return false
}

private fun Any.invokeNoArg(name: String): Any {
    val method =
        noArgMethod(name)
            ?: error(
                "SQLDelight model ${javaClass.name} does not expose $name(). " +
                    "Available no-arg methods: ${javaClass.noArgMethodNames().joinToString()}",
            )
    return method.invoke(this)
}

private fun Any.invokeNoArgOrNull(name: String): Any? = noArgMethod(name)?.invoke(this)

private fun Any.noArgMethod(name: String) = javaClass.methods.firstOrNull { method ->
    method.name == name && method.parameterCount == 0
}

private fun Any.unwrapGradleProvider(): Any =
    invokeNoArgOrNull("get") ?: this

private fun Any.asIterable(): Iterable<*> =
    when (this) {
        is Iterable<*> -> this
        is Array<*> -> asIterable()
        else ->
            error(
                "SQLDelight model ${javaClass.name} is not iterable. " +
                    "Available no-arg methods: ${javaClass.noArgMethodNames().joinToString()}",
            )
    }

private fun Class<*>.noArgMethodNames(): List<String> =
    methods
        .filter { method -> method.parameterCount == 0 }
        .map { method -> method.name }
        .distinct()
        .sorted()

private fun Any.sqlDelightCompilerClasspath(): List<File> {
    val taskClasspath =
        (invokeNoArg("getClasspath") as? FileCollection)
            ?.files
            ?.toList()
            .orEmpty()
    val implementationClasspath =
        generateSequence(javaClass as Class<*>?) { type -> type.superclass }
            .filter { type -> type.name.startsWith("app.cash.sqldelight.") }
            .mapNotNull { type -> type.protectionDomain.codeSource?.location?.toURI()?.let(::File) }
            .toList()
    val compilerApiClasspath =
        SQLDELIGHT_COMPILER_CLASSES.mapNotNull { className ->
            runCatching {
                Class
                    .forName(className, false, javaClass.classLoader)
                    .protectionDomain
                    .codeSource
                    ?.location
                    ?.toURI()
                    ?.let(::File)
            }.getOrNull()
        }
    return taskClasspath + implementationClasspath + compilerApiClasspath
}

private fun Configuration.directModuleDependencies(): List<ModuleDependency> =
    dependencies
        .filterIsInstance<ModuleDependency>()
        .filter { dependency -> dependency.group != null && dependency.version != null }

private fun Configuration.moduleComponents(): List<ModuleComponentIdentifier> =
    incoming
        .resolutionResult
        .allComponents
        .mapNotNull { component -> component.id as? ModuleComponentIdentifier }

private fun ModuleComponentIdentifier.isSqlDelightModule(moduleName: String): Boolean =
    group == SQLDELIGHT_GROUP && module == moduleName

private fun ModuleComponentIdentifier.isDialectArtifact(): Boolean =
    group == SQLDELIGHT_GROUP && module.endsWith(DIALECT_SUFFIX)

private fun String?.orElse(fallback: String): String = this ?: fallback
