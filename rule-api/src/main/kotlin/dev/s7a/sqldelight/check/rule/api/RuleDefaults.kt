package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.Enablement

/**
 * Default enablement derived from [Rule.defaultEnable].
 *
 * Rule implementations should override [Rule.defaultEnable] instead. This is
 * an extension property so rules cannot accidentally override the resolved
 * enablement contract: `true` maps to [Enablement.Auto], and `false` maps to
 * [Enablement.Disabled].
 */
public val Rule.defaultEnablement: Enablement
    get() = if (defaultEnable) Enablement.Auto else Enablement.Disabled
