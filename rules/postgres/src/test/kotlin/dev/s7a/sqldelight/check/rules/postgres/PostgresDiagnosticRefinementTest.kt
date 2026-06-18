package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialect
import dev.s7a.sqldelight.check.rules.standard.StandardRuleSetProvider
import kotlin.test.Test
import kotlin.test.assertTrue

class PostgresDiagnosticRefinementTest {
    @Test
    fun `postgres trigger function syntax does not report standard false positives`() {
        val diagnostics =
            diagnostics(
                """
                CREATE TRIGGER trigger_set_updated_at_on_files
                BEFORE UPDATE ON files
                FOR EACH ROW
                EXECUTE FUNCTION set_updated_at();
                """.trimIndent(),
            )
        val ruleIds = diagnostics.mapTo(mutableSetOf()) { it.ruleId }

        assertTrue(QualifiedRuleId("standard:no-update-without-where") !in ruleIds, diagnostics.describe())
        assertTrue(QualifiedRuleId("standard:source-indentation") !in ruleIds, diagnostics.describe())
    }

    @Test
    fun `postgres trigger body begin does not report statement terminator on trigger header`() {
        val diagnostics =
            diagnostics(
                """
                CREATE TRIGGER player_updated
                AFTER UPDATE ON player
                BEGIN
                  INSERT INTO player_log(player_id) VALUES (new.id);
                END;
                """.trimIndent(),
            )
        val ruleIds = diagnostics.mapTo(mutableSetOf()) { it.ruleId }

        assertTrue(QualifiedRuleId("standard:statement-terminator") !in ruleIds, diagnostics.describe())
    }

    @Test
    fun `postgres trigger body begin does not report migration transaction`() {
        val diagnostics =
            diagnostics(
                """
                CREATE TRIGGER player_updated
                AFTER UPDATE ON player
                BEGIN
                  INSERT INTO player_log(player_id) VALUES (new.id);
                END;
                """.trimIndent(),
                path = "src/main/sqldelight/Database/1.sqm",
            )
        val ruleIds = diagnostics.mapTo(mutableSetOf()) { it.ruleId }

        assertTrue(QualifiedRuleId("standard:no-transaction-in-migration") !in ruleIds, diagnostics.describe())
    }

    @Test
    fun `postgres trigger refinement keeps real missing statement terminator diagnostics`() {
        val diagnostics =
            diagnostics(
                """
                CREATE TRIGGER trigger_set_updated_at_on_files
                BEFORE UPDATE ON files
                FOR EACH ROW
                EXECUTE FUNCTION set_updated_at()
                """.trimIndent(),
            )
        val ruleIds = diagnostics.mapTo(mutableSetOf()) { it.ruleId }

        assertTrue(QualifiedRuleId("standard:statement-terminator") in ruleIds, diagnostics.describe())
    }

    private fun diagnostics(
        content: String,
        path: String = "src/main/sqldelight/Test.sq",
    ) =
        SqlDelightCheckEngine().run(
            inputs =
                listOf(
                    AnalysisInput(
                        database = DatabaseContext(name = "Database", dialect = PostgresDialect),
                        files = listOf(SourceFile(path = path, content = content)),
                    ),
                ),
            ruleSetProviders = listOf(StandardRuleSetProvider(), PostgresRuleSetProvider()),
        )
}

private fun List<Diagnostic>.describe(): String =
    joinToString("\n") { diagnostic ->
        "${diagnostic.ruleId} ${diagnostic.range}"
    }
