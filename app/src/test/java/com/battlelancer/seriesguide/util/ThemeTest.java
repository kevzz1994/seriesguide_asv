package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;

import junit.framework.Assert;

import org.junit.Test;

public class ThemeTest {

    @Test
    public void testThemeChange() {
        final String defaultThemeId = "1";

        ThemeUtils.updateTheme(defaultThemeId);
        int originalThemeId = SeriesGuidePreferences.THEME;

        String themeIdUpdated = "2";
        ThemeUtils.updateTheme(themeIdUpdated);
        int updatedThemeId = SeriesGuidePreferences.THEME;

        Assert.assertTrue("Theme didn't update",originalThemeId != updatedThemeId);
    }
}
