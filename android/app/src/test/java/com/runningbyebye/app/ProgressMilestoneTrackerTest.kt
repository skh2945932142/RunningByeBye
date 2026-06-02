package com.runningbyebye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressMilestoneTrackerTest {
    @Test
    fun triggersMilestonesOnce() {
        val tracker = ProgressMilestoneTracker()

        assertNull(tracker.hit(24))
        assertEquals(25, tracker.hit(25))
        assertNull(tracker.hit(26))
        assertEquals(50, tracker.hit(50))
        assertEquals(75, tracker.hit(99))
        assertEquals(100, tracker.hit(100))
        assertNull(tracker.hit(100))
    }

    @Test
    fun resetAllowsMilestonesAgain() {
        val tracker = ProgressMilestoneTracker()

        assertEquals(25, tracker.hit(30))
        tracker.reset()
        assertEquals(25, tracker.hit(30))
    }
}
