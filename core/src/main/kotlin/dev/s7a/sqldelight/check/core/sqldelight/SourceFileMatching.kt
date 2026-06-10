package dev.s7a.sqldelight.check.core.sqldelight

import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.api.SourceFile
import java.io.File

/**
 * Finds the sqldelight-check source file that corresponds to a SQLDelight compiler error path.
 */
internal fun AnalysisInput.findSourceFile(errorPath: String): SourceFile? {
    val normalizedErrorPath = File(errorPath).normalizeForComparison()
    return files.firstOrNull { file -> File(file.path).normalizeForComparison() == normalizedErrorPath }
        ?: files.firstOrNull { file -> normalizedErrorPath.endsWith("/${file.path.normalizePathSeparators()}") }
        ?: sourceFolders
            .asSequence()
            .mapNotNull { folder -> folder.relativePathFor(normalizedErrorPath) }
            .mapNotNull { relativePath -> files.findByRelativePath(relativePath) }
            .firstOrNull()
        ?: dependencyFolders
            .asSequence()
            .mapNotNull { folder -> folder.relativePathFor(normalizedErrorPath) }
            .mapNotNull { relativePath -> files.findByRelativePath(relativePath) }
            .firstOrNull()
}

private fun List<SourceFile>.findByRelativePath(relativePath: String): SourceFile? =
    firstOrNull { file -> file.path.normalizePathSeparators() == relativePath }
        ?: firstOrNull { file -> file.path.normalizePathSeparators().endsWith("/$relativePath") }

private fun File.relativePathFor(normalizedPath: String): String? {
    val folderPath = normalizeForComparison()
    if (normalizedPath == folderPath) return ""
    if (!normalizedPath.startsWith("$folderPath/")) return null
    return normalizedPath.removePrefix("$folderPath/")
}

private fun File.normalizeForComparison(): String =
    absoluteFile
        .toPath()
        .normalize()
        .toString()
        .normalizePathSeparators()

private fun String.normalizePathSeparators(): String = replace(File.separatorChar, '/')
