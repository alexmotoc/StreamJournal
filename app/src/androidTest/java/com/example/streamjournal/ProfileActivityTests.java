package com.example.streamjournal;

import android.content.Context;
import android.webkit.WebView;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ProfileActivityTests {
    @Rule
    public ActivityScenarioRule<ProfileActivity> activityScenarioRule
            = new ActivityScenarioRule<>(ProfileActivity.class);

    @Test
    public void twitch_click_shouldRenderWebView() {
        onView(withId(R.id.twitch_login)).perform(click());
        onView(withId(R.id.twitch_web)).check(matches(isDisplayed()));
    }

    @Test
    public void twitch_noClick_shouldNotRenderWebView() {
        onView(withId(R.id.twitch_web)).check(matches(not(isDisplayed())));
    }

    @Test
    public void twitch_doubleClick_shouldRemoveWebView() {
        onView(withId(R.id.twitch_login)).perform(click());
        onView(withId(R.id.twitch_login)).perform(click());
        onView(withId(R.id.twitch_web)).check(matches(not(isDisplayed())));
    }
}
