package com.pulselog.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.room.Room
import com.pulselog.MainActivity
import com.pulselog.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BloodPressureScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearRecords() {
        val db = Room.databaseBuilder(
            composeRule.activity.applicationContext,
            AppDatabase::class.java,
            "bp-db"
        )
            .allowMainThreadQueries()
            .build()
        runBlocking {
            db.bpDao().deleteAllRecords()
        }
        db.close()
    }

    @Test
    fun mainNavigationTagsAreAvailable() {
        composeRule.onNodeWithTag("main.tab.entry").assertIsDisplayed()
        composeRule.onNodeWithTag("main.tab.calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("main.tab.graph").assertIsDisplayed()
        composeRule.onNodeWithTag("main.header.settings").assertIsDisplayed()
        composeRule.onNodeWithTag("main.header.export").assertIsDisplayed()
    }

    @Test
    fun opensSettingsFromHeader() {
        composeRule.onNodeWithTag("main.header.settings").performClick()

        composeRule.onNodeWithTag("settings.morning.time").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.evening.time").assertIsDisplayed()
        composeRule.onNodeWithTag("settings.save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calendarTabExposesCalendarControls() {
        composeRule.onNodeWithTag("main.tab.calendar").performClick()

        composeRule.onNodeWithTag("calendar.month.previous").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar.month.next").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar.selected.open_entry").assertIsDisplayed()
    }

    @Test
    fun savesAndDeletesMorningRecord() {
        composeRule.onNodeWithTag("quick.morning.systolic").performTextClearance()
        composeRule.onNodeWithTag("quick.morning.systolic").performTextInput("121")
        composeRule.onNodeWithTag("quick.morning.diastolic").performTextClearance()
        composeRule.onNodeWithTag("quick.morning.diastolic").performTextInput("79")

        composeRule.onNodeWithTag("quick.morning.save").performClick()
        composeRule.waitForText("아침 혈압을 저장했습니다.")

        composeRule.onNodeWithTag("quick.morning.delete").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quick.delete.confirm").assertIsDisplayed().performClick()
        composeRule.waitForText("아침 혈압을 삭제했습니다.")
    }

    @Test
    fun invalidMorningInputShowsValidationMessage() {
        composeRule.onNodeWithTag("quick.morning.systolic").performTextClearance()
        composeRule.onNodeWithTag("quick.morning.systolic").performTextInput("49")
        composeRule.onNodeWithTag("quick.morning.diastolic").performTextClearance()
        composeRule.onNodeWithTag("quick.morning.diastolic").performTextInput("79")

        composeRule.onNodeWithTag("quick.morning.save").performClick()

        composeRule.waitForText("혈압은 50부터 200 사이여야 합니다.")
    }

    @Test
    fun savesAndDeletesEveningRecord() {
        composeRule.onNodeWithTag("quick.evening.systolic").performTextClearance()
        composeRule.onNodeWithTag("quick.evening.systolic").performTextInput("124")
        composeRule.onNodeWithTag("quick.evening.diastolic").performTextClearance()
        composeRule.onNodeWithTag("quick.evening.diastolic").performTextInput("82")

        composeRule.onNodeWithTag("quick.evening.save").performClick()
        composeRule.waitForText("저녁 혈압을 저장했습니다.")

        composeRule.onNodeWithTag("quick.evening.delete").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quick.delete.confirm").assertIsDisplayed().performClick()
        composeRule.waitForText("저녁 혈압을 삭제했습니다.")
    }

    @Test
    fun savesAndDeletesWeightRecord() {
        composeRule.onNodeWithTag("quick.weight.value").performTextClearance()
        composeRule.onNodeWithTag("quick.weight.value").performTextInput("65.4")

        composeRule.onNodeWithTag("quick.weight.save").performClick()
        composeRule.waitForText("체중을 저장했습니다.")

        composeRule.onNodeWithTag("quick.weight.delete").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quick.delete.confirm").assertIsDisplayed().performClick()
        composeRule.waitForText("체중을 삭제했습니다.")
    }

    @Test
    fun graphSelectionOpensSelectedDateInCalendar() {
        composeRule.onNodeWithTag("main.header.settings").performClick()
        composeRule.onNodeWithTag("settings.debug.seed_graph").performScrollTo().performClick()
        composeRule.waitForText("그래프 테스트 데이터를 추가했습니다.")
        composeRule.onNodeWithTag("main.header.settings").performClick()

        composeRule.waitForTag("main.tab.graph")
        composeRule.onNodeWithTag("main.tab.graph").performClick()
        composeRule.waitForTag("graph.blood_pressure.chart")
        composeRule.onNodeWithTag("graph.blood_pressure.chart")
            .performTouchInput { click(Offset(width * 0.55f, height * 0.45f)) }
        composeRule.waitForTag("graph.selected.open_calendar")
        composeRule.onNodeWithTag("graph.selected.open_calendar", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("calendar.month.previous").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar.month.next").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar.selected.open_entry").assertIsDisplayed()
    }

    private fun ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeTestRule.waitForTag(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
