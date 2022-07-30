package de.nox.dndassistant.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlin.test.assertEquals
import kotlin.test.assertNoEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

	@get:Rule
	var activityRule: ActivityTestRule<MainActivity>
		= ActivityTestRule(MainActivity::class.java)

	@Test
	fun rollHistory() {
		var size = Rollers.history.size
		var sizeDisplayed = Rollers.history.size

		/* Normal extra die view. */
		onView(withId(R.id.d4)).perform(click())

		/* Roller history increased. */
		assertNoEquals(size, Rollers.history.size, "Increase history entries.")

		/* Check, if view for history is updated. */
		onView(withId(R.id.list_rolls)).check(matches(isDisplayed()));

		/* Parse Term and roll it. */
		// onView(withId(R.id.dterm)).perform(typeText("D20 + -1"), closeSoftKeyboard())

		// onView(withId(R.id.text_result)).check(matches(isDisplayed()))
		// onView(withId(R.id.text_result)).check(matches(withText("1")))
	}

	@Test
	fun openPreviewContent() {
		var preview: View
		var content: View

		/* Check toggle view/content. */
		println("Toggle preview view: Show")
		preview.performClick()
		assertEquals(View.VISIBLE, content.visibility, "Content shown")

		println("Toggle preview view: Hide")
		preview.performClick()
		assertEquals(View.GONE, content.visibility, "Content hidden")
	}
}
