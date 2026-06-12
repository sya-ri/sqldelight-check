package dev.s7a.sqldelight.check.gradle

import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightTask
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.DialectRegistry
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Property

private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"
private const val DIALECT_SUFFIX = "-dialect"

/**
 * SQLDelight database input resolved from a Gradle project.
 */
internal data class ResolvedSqlDelightInput(
    /**
     * Core check input for sqldelight-check.
     */
    val analysisInput: AnalysisInput,
)

/**
 * Reads SQLDelight's generated Gradle task model through the supported SQLDelight 2.x task API.
 */
internal class SqlDelightProjectResolver(
    private val project: Project,
    private val dialectRegistry: DialectRegistry,
) {
    /**
     * Resolves SQLDelight database inputs known to the project.
     */
    fun resolve(): List<ResolvedSqlDelightInput> {
        val resolved =
            project.tasks
                .withType(SqlDelightTask::class.java)
                .map { task -> resolveTask(task) }

        return resolved
            .groupBy { input -> input.analysisInput.database.name }
            .map { (_, inputs) -> mergeResolvedSqlDelightInputs(inputs) }
    }

    private fun resolveTask(task: SqlDelightTask): ResolvedSqlDelightInput {
        val properties = task.properties.get()
        val compilationUnit = task.compilationUnit(properties)
        val databaseName = properties.className
        val dialectConfiguration = project.configurations.findByName("${databaseName}DialectClasspath")
        val dialect = resolveDialect(dialectConfiguration)
        val sourceFolders = resolveSourceFolders(compilationUnit)
        val localSourceFolders = sourceFolders.filterNot { folder -> folder.dependency }
        val sourceFiles = resolveSourceFiles(localSourceFolders)

        return ResolvedSqlDelightInput(
            analysisInput =
                AnalysisInput(
                    database = DatabaseContext(name = databaseName, dialect = dialect),
                    files = sourceFiles,
                ),
        )
    }

    private fun resolveSourceFolders(compilationUnit: SqlDelightCompilationUnitImpl): List<ResolvedSourceFolder> =
        compilationUnit
            .sourceFolders
            .map { sourceFolder ->
                ResolvedSourceFolder(file = sourceFolder.folder, dependency = sourceFolder.dependency)
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
                path = sourcePath(file),
                content = file.readText(StandardCharsets.UTF_8),
            )
        }
    }

    private fun sourcePath(file: File): String {
        val filePath = file.normalizedRealPath()
        val reportRootPath = reportRootPath()

        if (reportRootPath != null && filePath.startsWith(reportRootPath)) {
            return reportRootPath.relativize(filePath).toString().normalizePathSeparators()
        }

        return project.rootProject.relativePath(file)
    }

    private fun reportRootPath(): Path? =
        project.providers
            .gradleProperty("sqldelightCheck.reportRoot")
            .orNull
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { value -> project.file(value).normalizedRealPath() }
            ?: project.providers
                .environmentVariable("GITHUB_WORKSPACE")
                .orNull
                ?.takeIf { value -> value.isNotBlank() }
                ?.let { value -> File(value).normalizedRealPath() }

    private fun SqlDelightTask.compilationUnit(properties: SqlDelightDatabasePropertiesImpl): SqlDelightCompilationUnitImpl =
        compilationUnit.valueOrNull()
            ?: properties.compilationUnits.firstOrNull()
            ?: error("SQLDelight database properties ${properties.javaClass.name} do not expose a compilation unit.")

    private fun resolveDialect(configuration: Configuration?): SqlDialect {
        val directDialect = configuration?.directModuleDependencies()?.firstOrNull()
        if (directDialect != null) {
            return resolveDialectCoordinate(
                SqlDialectCoordinate(
                    group = directDialect.group.orEmpty(),
                    module = directDialect.name,
                    version = directDialect.version,
                ),
            )
        }

        val resolvedDialect =
            configuration
                ?.moduleComponents()
                ?.firstOrNull { component -> component.isDialectArtifact() }
        if (resolvedDialect != null) {
            return resolveDialectCoordinate(
                SqlDialectCoordinate(
                    group = resolvedDialect.group,
                    module = resolvedDialect.module,
                    version = resolvedDialect.version,
                ),
            )
        }

        return SqlDialect(family = DialectFamily.Unknown)
    }

    private fun resolveDialectCoordinate(coordinate: SqlDialectCoordinate): SqlDialect = dialectRegistry.resolve(coordinate)

}

/**
 * Combines SQLDelight inputs that Gradle exposes for the same logical database.
 *
 * SQLDelight can expose more than one task-backed input for a database. The
 * checker runs over the union of local files resolved from those task inputs.
 */
internal fun mergeResolvedSqlDelightInputs(inputs: List<ResolvedSqlDelightInput>): ResolvedSqlDelightInput {
    val first = inputs.first()
    inputs.drop(1).forEach { input ->
        require(input.analysisInput.database == first.analysisInput.database) {
            "Cannot merge SQLDelight inputs for different database contexts."
        }
    }

    return ResolvedSqlDelightInput(
        analysisInput =
            AnalysisInput(
                database = first.analysisInput.database,
                files = inputs.flatMap { input -> input.analysisInput.files }.distinctBy { file -> file.path }.sortedBy { file -> file.path },
            ),
    )
}

private data class ResolvedSourceFolder(
    val file: File,
    val dependency: Boolean,
)

private fun <T : Any> Property<T>.valueOrNull(): T? =
    if (isPresent) get() else null

private fun Configuration.directModuleDependencies(): List<ModuleDependency> =
    dependencies
        .filterIsInstance<ModuleDependency>()
        .filter { dependency -> dependency.group != null && dependency.version != null }

private fun Configuration.moduleComponents(): List<ModuleComponentIdentifier> =
    incoming
        .resolutionResult
        .allComponents
        .mapNotNull { component -> component.id as? ModuleComponentIdentifier }

private fun ModuleComponentIdentifier.isDialectArtifact(): Boolean =
    group == SQLDELIGHT_GROUP && module.endsWith(DIALECT_SUFFIX)

private fun File.normalizedRealPath(): Path {
    val path = toPath().toAbsolutePath().normalize()
    return runCatching { path.toRealPath() }.getOrDefault(path)
}

private fun String.normalizePathSeparators(): String = replace(File.separatorChar, '/')
