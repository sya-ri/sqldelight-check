package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports PostgreSQL index creation statements that omit CONCURRENTLY.
 *
 * Concurrent index builds reduce write blocking for indexes created on live
 * tables.
 */
public class RequireConcurrentIndexRule : RegexRule(
    ruleName = "require-concurrent-index",
    pattern = """\bCREATE\s+(?:UNIQUE\s+)?INDEX\b(?!\s+CONCURRENTLY\b)""",
    message = "Use CREATE INDEX CONCURRENTLY for PostgreSQL indexes that may be built on live tables.",
    targetCapability = DialectCapability.PostgreSql,
)
