package net.rpcs3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSafetyTest {
    @Test
    fun unknownBootResultFallsBackToGenericError() {
        assertEquals(BootResult.GenericError, BootResult.fromInt(Int.MAX_VALUE))
        assertEquals(BootResult.NoErrors, BootResult.fromInt(BootResult.NoErrors.ordinal))
    }

    @Test
    fun unknownEmulatorStateFallsBackToStopped() {
        assertEquals(EmulatorState.Stopped, EmulatorState.fromInt(Int.MAX_VALUE))
        assertEquals(EmulatorState.Running, EmulatorState.fromInt(EmulatorState.Running.ordinal))
    }

    @Test
    fun progressTerminalStatesAreStable() {
        assertTrue(ProgressUpdateEntry(10, 10, null).isComplete())
        assertTrue(ProgressUpdateEntry(-1, 0, null).isFailed())
        assertFalse(ProgressUpdateEntry(5, 10, null).isFinished())
        assertTrue(ProgressUpdateEntry(0, 0, null).isIndeterminate())
    }
}
