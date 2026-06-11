package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for provider registry validation before task execution starts.
 */
class ProviderRegistryTest {
    @Test
    fun `reporter registry rejects duplicate reporter IDs`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                ReporterRegistry(
                    listOf(
                        reporterProvider("json"),
                        reporterProvider("json"),
                    ),
                )
            }

        assertEquals("Duplicate sqldelight-check reporter provider ID(s): json", error.message)
    }

    @Test
    fun `rule registry rejects duplicate rule set IDs`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                RuleRegistry.create(
                    listOf(
                        ruleSetProvider("standard", "standard:first"),
                        ruleSetProvider("standard", "standard:second"),
                    ),
                )
            }

        assertEquals("Duplicate sqldelight-check rule set provider ID(s): standard", error.message)
    }

    @Test
    fun `rule registry rejects duplicate rule IDs`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                RuleRegistry.create(
                    listOf(
                        ruleSetProvider("custom", "duplicate", "duplicate"),
                    ),
                )
            }

        assertEquals("Duplicate sqldelight-check rule ID(s): custom:duplicate in custom", error.message)
    }

    @Test
    fun `rule registry allows matching local rule IDs across different rule sets`() {
        RuleRegistry.create(
            listOf(
                ruleSetProvider("first", "duplicate"),
                ruleSetProvider("second", "duplicate"),
            ),
        )
    }

    @Test
    fun `rule registry rejects full rule IDs from rules`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                RuleRegistry.create(
                    listOf(
                        ruleSetProvider("custom", "custom:duplicate"),
                    ),
                )
            }

        assertEquals("Rule IDs must be local and must not contain ':': custom:custom:duplicate", error.message)
    }

    private fun reporterProvider(id: String): ReporterProvider =
        object : ReporterProvider {
            override val id: String = id

            override fun create(options: Map<String, String>): Reporter =
                object : Reporter {
                    override fun write(
                        report: Report,
                        output: ReportOutput,
                    ) = Unit
                }
        }

    private fun ruleSetProvider(
        ruleSetId: String,
        vararg ruleIds: String,
    ): RuleSetProvider =
        object : RuleSetProvider {
            override val id: RuleSetId = RuleSetId(ruleSetId)

            override fun ruleProviders(): Set<RuleProvider> =
                ruleIds
                    .map { ruleId ->
                        RuleProvider {
                            testRule(ruleId)
                        }
                    }.toSet()
        }

    private fun testRule(ruleId: String): Rule =
        object : Rule {
            override val id: String = ruleId
            override val defaultSeverity: Severity = Severity.Warning
            override val defaultEnable: Boolean = true

            override fun run(
                context: RuleContext,
                reporter: DiagnosticReporter,
            ) = Unit
        }
}
