package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import kotlin.test.assertEquals

internal const val PLAYER_SQ_PATH: String = "src/main/sqldelight/com/example/Player.sq"

internal const val MIGRATION_SQM_PATH: String = "src/main/sqldelight/com/example/1.sqm"

internal val cleanPlayerSq: String =
    """
    import com.example.PlayerId;

    CREATE TABLE player (
      id INTEGER AS PlayerId NOT NULL PRIMARY KEY,
      name TEXT NOT NULL,
      score INTEGER NOT NULL DEFAULT 0
    );

    CREATE INDEX player_name ON player(name);

    selectAll:
    SELECT id, name, score
    FROM player
    ORDER BY name;

    selectById:
    SELECT id, name, score
    FROM player
    WHERE id = ?;

    insertPlayer:
    INSERT INTO player(id, name, score)
    VALUES (?, ?, ?);
    """.trimIndent() + "\n"

internal val cleanMigrationSqm: String =
    """
    CREATE TABLE player (
      id INTEGER NOT NULL PRIMARY KEY,
      name TEXT NOT NULL
    );

    ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
    """.trimIndent() + "\n"

internal fun Rule.diagnostics(
    content: String,
    path: String = PLAYER_SQ_PATH,
): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    run(
        context =
            object : RuleContext {
                override val database: DatabaseContext =
                    DatabaseContext(
                        name = "Database",
                        dialect = SqlDialect(family = DialectFamily.SQLite, displayName = "SQLite"),
                    )
                override val file: SourceFile = SourceFile(path = path, content = content)
            },
        reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic },
    )
    return diagnostics
}

internal fun Rule.assertDiagnosticCount(
    content: String,
    expected: Int,
    path: String = PLAYER_SQ_PATH,
) {
    assertEquals(expected, diagnostics(content, path).size)
}

internal fun Rule.singleReplacement(
    content: String,
    path: String = PLAYER_SQ_PATH,
): String =
    diagnostics(content, path)
        .single()
        .fixes
        .single()
        .edits
        .single()
        .replacement

internal fun String.asSqlDelightFile(): String = trimIndent() + "\n"

internal fun String.withTabs(): String = replace("<TAB>", "\t")

internal fun String.withSpaces(): String = replace("<SP>", " ")
