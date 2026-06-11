package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports CREATE INDEX CONCURRENTLY inside transaction blocks.
 *
 * PostgreSQL rejects concurrent index creation inside an explicit transaction.
 */
public class NoConcurrentIndexInTransactionRule : RegexRule(
    ruleName = "no-concurrent-index-in-transaction",
    pattern = """\bBEGIN\b[\s\S]*\bCREATE\s+(?:UNIQUE\s+)?INDEX\s+CONCURRENTLY\b|\bCREATE\s+(?:UNIQUE\s+)?INDEX\s+CONCURRENTLY\b[\s\S]*\bCOMMIT\b""",
    message = "CREATE INDEX CONCURRENTLY cannot run inside a transaction block.",
    targetCapability = DialectCapability.PostgreSql,
)
