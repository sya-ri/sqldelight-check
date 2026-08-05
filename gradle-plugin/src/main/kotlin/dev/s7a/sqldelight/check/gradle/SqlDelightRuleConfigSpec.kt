package dev.s7a.sqldelight.check.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

/**
 * Gradle-serializable specification for one rule set configuration entry.
 */
public abstract class RuleSetConfigSpec {
    /** Rule set ID, e.g. `"standard"`. */
    @get:Input
    public abstract val id: Property<String>

    /**
     * Explicit enablement override. Absent means "use the rule's default".
     * `true` forces the rule set on; `false` forces it off.
     */
    @get:Input
    @get:Optional
    public abstract val enabled: Property<Boolean>
}

/**
 * Gradle-serializable specification for one rule configuration entry.
 */
public abstract class RuleConfigSpec {
    /** Qualified rule ID as `"ruleSetId:ruleId"`. */
    @get:Input
    public abstract val id: Property<String>

    /**
     * Explicit enablement override. Absent means "use the rule's default".
     */
    @get:Input
    @get:Optional
    public abstract val enabled: Property<Boolean>

    /** Severity name matching `Severity.name`. */
    @get:Input
    public abstract val severity: Property<String>

    /** Rule-specific string options. */
    @get:Input
    public abstract val options: MapProperty<String, String>
}

/**
 * Gradle-serializable specification for database-specific configuration overrides.
 */
public abstract class DatabaseConfigSpec {
    /** SQLDelight database name. */
    @get:Input
    public abstract val name: Property<String>

    /** Database-level rule set overrides. */
    @get:Nested
    public abstract val ruleSets: ListProperty<RuleSetConfigSpec>

    /** Database-level rule overrides. */
    @get:Nested
    public abstract val rules: ListProperty<RuleConfigSpec>
}
