package dev.s7a.sqldelight.check.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightTask
import com.alecstrong.sql.psi.core.SqlCoreEnvironment
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property

private const val DEFAULT_SQLDELIGHT_VERSION = "2.3.2"
private val SQLDELIGHT_COMPILER_CLASSES =
    listOf(
        SqlDelightCompilationUnit::class.java,
        SqlDelightDatabaseProperties::class.java,
        SqlDelightSourceFolder::class.java,
        SqlDelightDialect::class.java,
        SqlCoreEnvironment::class.java,
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
 * Reads SQLDelight's generated Gradle task model through the supported SQLDelight 2.x task API.
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
                .withType(SqlDelightTask::class.java)
                .mapNotNull { task -> resolveTask(task) }

        return resolved
            .groupBy { input -> input.analysisInput.database.name }
            .map { (_, inputs) -> mergeResolvedSqlDelightInputs(inputs) }
    }

    private fun resolveTask(task: SqlDelightTask): ResolvedSqlDelightInput? {
        val properties = task.properties.get()
        val compilationUnit = task.compilationUnit(properties)
        val databaseName = properties.className
        val packageName = properties.packageName
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

/**
 * Combines SQLDelight inputs that Gradle exposes for the same logical database.
 *
 * SQLDelight can expose more than one task-backed input for a database. The
 * analyzer must see every source and dependency folder that contributed files;
 * otherwise reports can mention files that SQLDelight did not parse.
 */
internal fun mergeResolvedSqlDelightInputs(inputs: List<ResolvedSqlDelightInput>): ResolvedSqlDelightInput {
    val first = inputs.first()
    inputs.drop(1).forEach { input ->
        require(input.analysisInput.database == first.analysisInput.database) {
            "Cannot merge SQLDelight inputs for different database contexts."
        }
        require(input.sqlDelightVersion == first.sqlDelightVersion) {
            "Cannot merge SQLDelight inputs for ${first.analysisInput.database.name} with different SQLDelight versions."
        }
        require(input.analysisInput.packageName == first.analysisInput.packageName) {
            "Cannot merge SQLDelight inputs for ${first.analysisInput.database.name} with different package names."
        }
    }

    return ResolvedSqlDelightInput(
        sqlDelightVersion = first.sqlDelightVersion,
        analysisInput =
            AnalysisInput(
                database = first.analysisInput.database,
                files = inputs.flatMap { input -> input.analysisInput.files }.distinctBy { file -> file.path }.sortedBy { file -> file.path },
                sqlDelightVersion = first.analysisInput.sqlDelightVersion,
                packageName = first.analysisInput.packageName,
                sourceFolders = inputs.flatMap { input -> input.analysisInput.sourceFolders }.distinctBy { file -> file.absolutePath },
                dependencyFolders = inputs.flatMap { input -> input.analysisInput.dependencyFolders }.distinctBy { file -> file.absolutePath },
                compilerClasspath = inputs.flatMap { input -> input.analysisInput.compilerClasspath }.distinctBy { file -> file.absolutePath },
                dialectClasspath = inputs.flatMap { input -> input.analysisInput.dialectClasspath }.distinctBy { file -> file.absolutePath },
            ),
    )
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

private fun SqlDelightTask.sqlDelightCompilerClasspath(): List<File> {
    val taskClasspath =
        (classpath as? FileCollection)
            ?.files
            ?.toList()
            .orEmpty()
    val implementationClasspath =
        generateSequence(javaClass as Class<*>?) { type -> type.superclass }
            .filter { type -> type.name.startsWith("app.cash.sqldelight.") }
            .mapNotNull { type -> type.protectionDomain.codeSource?.location?.toURI()?.let(::File) }
            .toList()
    val compilerApiClasspath =
        SQLDELIGHT_COMPILER_CLASSES.mapNotNull { type ->
            type.protectionDomain.codeSource?.location?.toURI()?.let(::File)
        }
    return taskClasspath + implementationClasspath + compilerApiClasspath
}

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

private fun ModuleComponentIdentifier.isSqlDelightModule(moduleName: String): Boolean =
    group == SQLDELIGHT_GROUP && module == moduleName

private fun ModuleComponentIdentifier.isDialectArtifact(): Boolean =
    group == SQLDELIGHT_GROUP && module.endsWith(DIALECT_SUFFIX)

private fun String?.orElse(fallback: String): String = this ?: fallback

private fun File.normalizedRealPath(): Path {
    val path = toPath().toAbsolutePath().normalize()
    return runCatching { path.toRealPath() }.getOrDefault(path)
}

private fun String.normalizePathSeparators(): String = replace(File.separatorChar, '/')
