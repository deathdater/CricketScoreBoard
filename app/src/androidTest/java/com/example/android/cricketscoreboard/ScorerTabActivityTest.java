package com.example.android.cricketscoreboard;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager2.widget.ViewPager2;

import com.example.android.cricketscoreboard.views.ScorerTabActivity;
import com.google.android.material.tabs.TabLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies the ViewPager2 + TabLayoutMediator wiring in {@link ScorerTabActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class ScorerTabActivityTest {

    @Test
    public void tabsAreWiredToPager() {
        try (ActivityScenario<ScorerTabActivity> scenario =
                     ActivityScenario.launch(ScorerTabActivity.class)) {
            scenario.onActivity(activity -> {
                ViewPager2 pager = activity.findViewById(R.id.container);
                TabLayout tabs = activity.findViewById(R.id.tabs);
                assertNotNull(pager.getAdapter());
                assertEquals(4, pager.getAdapter().getItemCount());
                assertEquals(4, tabs.getTabCount());
                assertEquals("Scorer Pad", String.valueOf(tabs.getTabAt(0).getText()));
                assertEquals("Match Summary", String.valueOf(tabs.getTabAt(3).getText()));

                pager.setCurrentItem(2, false);
                assertEquals(2, tabs.getSelectedTabPosition());
            });
        }
    }
}
