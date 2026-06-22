package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferImportedMappedTypeRuleTest {
    @Test
    fun `reports fully qualified mapped type and adds import`() {
        val content =
            """
            CREATE TABLE user (
              id TEXT AS com.example.UserId NOT NULL,
              owner_id TEXT AS com.example.UserId
            );
            """.asSqlDelightFile()
        val diagnostics = PreferImportedMappedTypeRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(
            """
            import com.example.UserId;

            CREATE TABLE user (
              id TEXT AS UserId NOT NULL,
              owner_id TEXT AS UserId
            );
            """.asSqlDelightFile(),
            PreferImportedMappedTypeRule().applyAllFixes(content),
        )
    }

    @Test
    fun `adds import to existing sorted import block`() {
        val content =
            """
            import java.time.Instant;
            import com.example.UserName;

            CREATE TABLE user (
              id TEXT AS com.example.UserId NOT NULL,
              name TEXT AS UserName NOT NULL
            );
            """.asSqlDelightFile()

        assertEquals(
            """
            import java.time.Instant;
            import com.example.UserId;
            import com.example.UserName;

            CREATE TABLE user (
              id TEXT AS UserId NOT NULL,
              name TEXT AS UserName NOT NULL
            );
            """.asSqlDelightFile(),
            PreferImportedMappedTypeRule().applyAllFixes(content),
        )
    }

    @Test
    fun `fixes multiple fully qualified mapped types together`() {
        val content =
            """
            CREATE TABLE event (
              id TEXT AS com.example.EventId NOT NULL,
              created_at TEXT AS kotlin.time.Instant NOT NULL
            );
            """.asSqlDelightFile()
        val diagnostics = PreferImportedMappedTypeRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(
            """
            import kotlin.time.Instant;
            import com.example.EventId;

            CREATE TABLE event (
              id TEXT AS EventId NOT NULL,
              created_at TEXT AS Instant NOT NULL
            );
            """.asSqlDelightFile(),
            PreferImportedMappedTypeRule().applyAllFixes(content),
        )
    }

    @Test
    fun `imports outer generic mapped type only`() {
        val content =
            """
            CREATE TABLE event (
              tags TEXT AS kotlin.collections.List<com.example.Tag> NOT NULL
            );
            """.asSqlDelightFile()

        assertEquals(
            """
            import kotlin.collections.List;

            CREATE TABLE event (
              tags TEXT AS List<com.example.Tag> NOT NULL
            );
            """.asSqlDelightFile(),
            PreferImportedMappedTypeRule().applyAllFixes(content),
        )
    }

    @Test
    fun `accepts imported wildcard and conflicting simple names`() {
        PreferImportedMappedTypeRule().assertDiagnosticCount(
            """
            import com.example.*;

            CREATE TABLE user (
              id TEXT AS com.example.UserId NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
        PreferImportedMappedTypeRule().assertDiagnosticCount(
            """
            import com.other.UserId;

            CREATE TABLE user (
              id TEXT AS com.example.UserId NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
        PreferImportedMappedTypeRule().assertDiagnosticCount(
            """
            CREATE TABLE event (
              created_at TEXT AS com.example.Instant NOT NULL,
              updated_at TEXT AS kotlin.time.Instant NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `accepts simple mapped types and ignores migrations`() {
        val content =
            """
            CREATE TABLE user (
              id TEXT AS UserId NOT NULL
            );
            """.asSqlDelightFile()

        PreferImportedMappedTypeRule().assertDiagnosticCount(content, expected = 0)
        PreferImportedMappedTypeRule().assertDiagnosticCount(
            """
            CREATE TABLE user (
              id TEXT AS com.example.UserId NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
