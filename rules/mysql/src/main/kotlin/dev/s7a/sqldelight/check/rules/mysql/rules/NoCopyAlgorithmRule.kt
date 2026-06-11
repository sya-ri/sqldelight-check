package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.rule.api.RegexRule

/**
 * Reports MySQL ALTER TABLE statements that force the COPY algorithm.
 *
 * COPY can rebuild the table and is usually unsafe for online schema changes.
 */
public class NoCopyAlgorithmRule : RegexRule(
    ruleName = "no-copy-algorithm",
    pattern = """\bALGORITHM\s*=\s*COPY\b""",
    message = "Avoid MySQL ALTER TABLE ALGORITHM=COPY for online migrations.",
    targetCapability = DialectCapability.MySql,
    hashLineComments = true,
)
