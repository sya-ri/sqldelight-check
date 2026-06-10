package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import java.io.File
import java.nio.charset.StandardCharsets
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

private const val SQLDELIGHT_TASK_CLASS_NAME = "app.cash.sqldelight.gradle.SqlDelightTask"
private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"
private const val DEFAULT_SQLDELIGHT_VERSION = "2.3.2"

/**
 * SQLDelight database input resolved from a Gradle project.
 */
internal data class ResolvedSqlDelightInput(
    /** SQLDelight version used for this database. */
    val sqlDelightVersion: String,
    /** Adapter input for sqldelight-check analysis. */
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
        val properties = task.invokeNoArg("getProperties").invokeNoArg("get")
        val compilationUnit = task.invokeNoArg("getCompilationUnit").invokeNoArg("get")
        val databaseName = properties.invokeNoArg("getClassName") as String
        val dialectConfiguration = project.configurations.findByName("${databaseName}DialectClasspath")
        val intellijConfiguration = project.configurations.findByName("${databaseName}IntellijEnv")
        val dialect = resolveDialect(dialectConfiguration)
        val version = resolveSqlDelightVersion(intellijConfiguration, dialect).orElse(DEFAULT_SQLDELIGHT_VERSION)
        val sourceFiles = resolveSourceFiles(compilationUnit)

        return ResolvedSqlDelightInput(
            sqlDelightVersion = version,
            analysisInput =
                AnalysisInput(
                    database = DatabaseContext(name = databaseName, dialect = dialect),
                    files = sourceFiles,
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

    private fun resolveSourceFiles(compilationUnit: Any): List<SourceFile> {
        val sourceFolders = compilationUnit.invokeNoArg("getSourceFolders") as Iterable<*>
        return sourceFolders
            .mapNotNull { sourceFolder -> sourceFolder?.invokeNoArg("getFolder") as? File }
            .flatMap { folder -> sqlDelightFiles(folder) }
            .distinctBy { file -> file.path }
            .sortedBy { file -> file.path }
    }

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

    private fun resolveSqlDelightVersion(
        configuration: Configuration?,
        dialect: SqlDialect,
    ): String? {
        val compilerEnvVersion =
            configuration
                ?.moduleComponents()
                ?.firstOrNull { component ->
                    component.group == SQLDELIGHT_GROUP && component.module == "compiler-env"
                }
                ?.version
        return compilerEnvVersion ?: dialect.version
    }

    private fun resolveDialect(configuration: Configuration?): SqlDialect {
        val directDialect = configuration?.directModuleDependencies()?.firstOrNull()
        if (directDialect != null) {
            return dialectFromCoordinate(
                group = directDialect.group.orEmpty(),
                module = directDialect.name,
                version = directDialect.version,
            )
        }

        val resolvedDialect =
            configuration
                ?.moduleComponents()
                ?.firstOrNull { component -> component.module.endsWith("-dialect") }
        if (resolvedDialect != null) {
            return dialectFromCoordinate(
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

    private fun dialectFromCoordinate(
        group: String,
        module: String,
        version: String?,
    ): SqlDialect {
        val artifact = "$group:$module"
        return when {
            group == SQLDELIGHT_GROUP && module.startsWith("sqlite-") && module.endsWith("-dialect") ->
                SqlDialect(
                    family = DialectFamily.SQLite,
                    displayName = module.removeSuffix("-dialect").replace('-', ' '),
                    artifact = artifact,
                    version = version,
                    capabilities = setOf("sqlite"),
                )
            group == SQLDELIGHT_GROUP && module == "mysql-dialect" ->
                SqlDialect(
                    family = DialectFamily.MySql,
                    displayName = "MySQL",
                    artifact = artifact,
                    version = version,
                    capabilities = setOf("mysql"),
                )
            group == SQLDELIGHT_GROUP && module == "postgresql-dialect" ->
                SqlDialect(
                    family = DialectFamily.PostgreSql,
                    displayName = "PostgreSQL",
                    artifact = artifact,
                    version = version,
                    capabilities = setOf("postgresql"),
                )
            group == SQLDELIGHT_GROUP && module == "hsql-dialect" ->
                SqlDialect(
                    family = DialectFamily.Hsql,
                    displayName = "HSQL",
                    artifact = artifact,
                    version = version,
                    capabilities = setOf("hsql"),
                )
            else ->
                SqlDialect(
                    family = DialectFamily.Custom,
                    displayName = module,
                    artifact = artifact,
                    version = version,
                )
        }
    }
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
    val method = javaClass.methods.first { method -> method.name == name && method.parameterCount == 0 }
    return method.invoke(this)
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

private fun String?.orElse(fallback: String): String = this ?: fallback
