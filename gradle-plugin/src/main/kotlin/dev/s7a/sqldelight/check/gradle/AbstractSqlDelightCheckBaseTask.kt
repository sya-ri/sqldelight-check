package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.DialectRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

/**
 * Shared base for sqldelight-check tasks.
 *
 * Declares all configuration-cache-compatible properties that are common to every
 * check/baseline task, and provides protected helpers that forward to the
 * package-level functions in TaskAnalysisHelpers.kt.
 */
public abstract class AbstractSqlDelightCheckBaseTask : DefaultTask() {
    /**
     * Log output detail for this task execution.
     */
    @get:Input
    public abstract val logLevel: Property<LogLevel>

    /**
     * Runtime classpath used to discover rule set providers.
     */
    @get:Classpath
    public abstract val ruleSetClasspath: ConfigurableFileCollection

    /**
     * Runtime classpath used to discover dialect metadata providers.
     */
    @get:Classpath
    public abstract val dialectClasspath: ConfigurableFileCollection

    // ── database and config inputs ──────────────────────────────────────────

    /**
     * Per-database source files and dialect coordinates, captured at configuration time.
     */
    @get:Nested
    public abstract val databases: ListProperty<SqlDelightDatabaseSpec>

    /**
     * Global rule set configuration, captured at configuration time.
     */
    @get:Nested
    public abstract val globalRuleSets: ListProperty<RuleSetConfigSpec>

    /**
     * Global rule configuration, captured at configuration time.
     */
    @get:Nested
    public abstract val globalRules: ListProperty<RuleConfigSpec>

    /**
     * Database-specific configuration overrides, captured at configuration time.
     */
    @get:Nested
    public abstract val databaseConfigs: ListProperty<DatabaseConfigSpec>

    // ── path resolution inputs ───────────────────────────────────────────────

    /**
     * Absolute path of the root project directory, for path relativization.
     */
    @get:Internal
    public abstract val rootProjectDir: Property<String>

    /**
     * Override root for report paths (from `sqldelightCheck.reportRoot` Gradle property
     * or `GITHUB_WORKSPACE` env var). Empty string means "not set".
     */
    @get:Input
    @get:Optional
    public abstract val reportRoot: Property<String>

    // ── protected helpers ─────────────────────────────────────────────────────

    protected fun buildRuleRegistry(): RuleRegistry = buildRuleRegistry(ruleSetClasspath)

    protected fun buildDialectRegistry(): DialectRegistry = buildDialectRegistry(dialectClasspath)

    protected fun buildAnalysisInputs(dialectRegistry: DialectRegistry): List<AnalysisInput> =
        buildAnalysisInputs(
            databases = databases.get(),
            reportRoot = reportRoot.orNull,
            rootProjectDir = rootProjectDir.get(),
            dialectRegistry = dialectRegistry,
        )
}
