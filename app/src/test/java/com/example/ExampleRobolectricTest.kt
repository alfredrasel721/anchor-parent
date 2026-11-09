package com.example

import com.example.viewmodel.AnchorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun freshParentStateHasNoReusableOrPersistedPairingSecret() {
        val state = AnchorUiState()

        assertEquals("NONE", state.pairingCode.status)
        assertEquals("", state.pairingCode.code)
        assertEquals("", state.pairingCode.codeId)
        assertEquals(0, state.pairingCountdownSeconds)
        assertTrue(state.pairedDevices.isEmpty())
    }
}
