package dev.s7a.sqldelight.check.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QualifiedRuleIdTest {
    @Test
    fun `string constructor reads rule set and rule id`() {
        val id = QualifiedRuleId("standard:no-select-star")

        assertEquals(RuleSetId("standard"), id.ruleSetId)
        assertEquals(RuleId("no-select-star"), id.ruleId)
        assertEquals("standard:no-select-star", id.value)
    }

    @Test
    fun `string constructor rejects unqualified values`() {
        assertFailsWith<IllegalArgumentException> {
            QualifiedRuleId("no-select-star")
        }
    }

    @Test
    fun `string constructor rejects missing rule set or rule id`() {
        assertFailsWith<IllegalArgumentException> {
            QualifiedRuleId(":no-select-star")
        }
        assertFailsWith<IllegalArgumentException> {
            QualifiedRuleId("standard:")
        }
    }
}
