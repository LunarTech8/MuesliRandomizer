package com.romanbrunner.apps.mueslirandomizer;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/**
 * Minimal UI sanity test for MainActivity. Verifies app context and basic UI presence.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUiSanityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
        new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.romanbrunner.apps.mueslirandomizer", appContext.getPackageName());
    }

    @Test
    public void mainUiElementsAreVisible() {
        onView(withId(R.id.mixMuesliButton)).check(matches(isDisplayed()));
        onView(withId(R.id.availabilityButton)).check(matches(isDisplayed()));
        onView(withId(R.id.editItemsButton)).check(matches(isDisplayed()));
    }
}
