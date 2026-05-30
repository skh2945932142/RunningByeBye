package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RunServiceStateTest {
    @Test
    fun observerReceivesCurrentStateAndUpdatesUntilRemoved() {
        RunServiceState.resetForTest()
        val seen = mutableListOf<RunStatus>()
        val observer = RunStatusObserver { status -> seen += status }

        RunServiceState.observe(observer)
        RunServiceState.update(RunStatus.Running("风华运动场"))
        RunServiceState.update(RunStatus.Progress(8, 749, 0.123))
        RunServiceState.removeObserver(observer)
        RunServiceState.update(RunStatus.Completed(2.2))

        assertEquals(
            listOf(
                RunStatus.Idle,
                RunStatus.Running("风华运动场"),
                RunStatus.Progress(8, 749, 0.123),
            ),
            seen,
        )
    }

    @Test
    fun resetForTestRestoresIdleAndRemovesObservers() {
        RunServiceState.resetForTest()
        val seen = mutableListOf<RunStatus>()
        val observer = RunStatusObserver { status -> seen += status }

        RunServiceState.observe(observer)
        RunServiceState.update(RunStatus.Failed("network down"))
        RunServiceState.resetForTest()
        assertEquals(RunStatus.Idle, RunServiceState.current())

        RunServiceState.update(RunStatus.Running("太极运动场"))

        assertEquals(listOf(RunStatus.Idle, RunStatus.Failed("network down")), seen)
    }
}
