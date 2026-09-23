package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.quran.QuranKhatmGoal
import com.example.quran.QuranKhatmPlanner
import com.example.quran.QuranKhatmRepository
import com.example.quran.QuranKhatmStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranKhatmPlannerTest {
    private val startDay = QuranKhatmPlanner.dayKey(1_725_235_200_000L)

    @Test
    fun thirtyDayPlanAllocatesTheFirstDailyRange() {
        val goal = QuranKhatmGoal(
            startedDayKey = startDay,
            targetDays = 30,
            startPage = 1,
            lastCompletedPage = 0,
            reminderEnabled = true,
            reminderTime = "20:00"
        )

        val plan = QuranKhatmPlanner.plan(goal, startDay)

        assertEquals(1, plan.dayNumber)
        assertEquals(1, plan.targetStartPage)
        assertEquals(21, plan.targetEndPage)
        assertEquals(QuranKhatmStatus.ON_TRACK, plan.status)
    }

    @Test
    fun missedDaysIncreaseTheRemainingDailyTarget() {
        val goal = QuranKhatmGoal(
            startedDayKey = startDay,
            targetDays = 30,
            startPage = 1,
            lastCompletedPage = 0,
            reminderEnabled = true,
            reminderTime = "20:00"
        )

        val plan = QuranKhatmPlanner.plan(goal, QuranKhatmPlanner.addDays(startDay, 2))

        assertEquals(3, plan.dayNumber)
        assertEquals(22, plan.targetEndPage)
        assertEquals(QuranKhatmStatus.BEHIND, plan.status)
    }

    @Test
    fun repositoryPersistsProgressAndDailyLogs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranKhatmRepository(context)
        repository.clearGoal()
        repository.createGoal(60, 100, true, "19:30", startDay)

        val updated = repository.recordProgress(125, startDay)
        val recreated = QuranKhatmRepository(context)

        assertEquals(125, updated?.lastCompletedPage)
        assertEquals(125, recreated.getGoal()?.lastCompletedPage)
        assertEquals("19:30", recreated.getGoal()?.reminderTime)
        assertTrue(recreated.getDailyLogs().any { it.dayKey == startDay && it.completedThroughPage == 125 })
        repository.clearGoal()
    }

    @Test
    fun finalPageCompletesTheGoal() {
        val goal = QuranKhatmGoal(
            startedDayKey = startDay,
            targetDays = 7,
            startPage = 600,
            lastCompletedPage = 604,
            reminderEnabled = false,
            reminderTime = "20:00",
            completedDayKey = startDay
        )

        val plan = QuranKhatmPlanner.plan(goal, startDay)

        assertEquals(1f, plan.progress)
        assertEquals(QuranKhatmStatus.COMPLETE, plan.status)
    }

    @Test
    fun pauseFreezesThePlanAndResumeExtendsItsDeadline() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranKhatmRepository(context)
        repository.clearGoal()
        repository.createGoal(30, 1, false, "20:00", startDay)
        val pauseDay = QuranKhatmPlanner.addDays(startDay, 2)
        repository.setPaused(true, pauseDay)

        val paused = requireNotNull(repository.getGoal())
        val later = QuranKhatmPlanner.addDays(pauseDay, 5)
        assertEquals(3, QuranKhatmPlanner.plan(paused, later).dayNumber)

        val resumed = requireNotNull(repository.setPaused(false, later))
        assertEquals(3, QuranKhatmPlanner.plan(resumed, later).dayNumber)
        assertEquals(
            QuranKhatmPlanner.addDays(startDay, 34),
            QuranKhatmPlanner.plan(resumed, later).deadlineDayKey
        )
        repository.clearGoal()
    }

    @Test
    fun progressBeforeStartPageDoesNotCreateAReadingLog() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranKhatmRepository(context)
        repository.clearGoal()
        repository.createGoal(30, 100, false, "20:00", startDay)

        assertEquals(99, repository.recordProgress(50, startDay)?.lastCompletedPage)
        assertTrue(repository.getDailyLogs().isEmpty())
        repository.clearGoal()
    }
}
