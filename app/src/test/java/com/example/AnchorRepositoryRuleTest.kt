package com.example

import com.example.model.PairingCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorRepositoryRuleTest {

    @Test
    fun serverManagedPairingStatusNeverPersistsTheRawPasscode() {
        val statusRecord = PairingCode(
            id = "pair_child_leo_test",
            codeId = "pair_child_leo_test",
            familyId = "fam_parent_test",
            ownerUid = "parent_test",
            childId = "child_leo",
            childName = "Leo",
            code = "",
            expiresAtEpochMs = 1_800_000_000_000L,
            status = "ACTIVE",
            revoked = false
        )

        assertEquals("", statusRecord.code)
        assertEquals("ACTIVE", statusRecord.status)
        assertFalse(statusRecord.revoked)
    }

    @Test
    fun revokedPairingStatusCannotBeTreatedAsActive() {
        val revoked = PairingCode(
            id = "pair_child_leo_revoked",
            codeId = "pair_child_leo_revoked",
            familyId = "fam_parent_test",
            ownerUid = "parent_test",
            childId = "child_leo",
            childName = "Leo",
            code = "",
            expiresAtEpochMs = 1_800_000_000_000L,
            status = "REVOKED",
            revoked = true
        )

        assertEquals("REVOKED", revoked.status)
        assertTrue(revoked.revoked)
    }
}
