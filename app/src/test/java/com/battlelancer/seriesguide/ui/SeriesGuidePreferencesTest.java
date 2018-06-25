package com.battlelancer.seriesguide.ui;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

/**
 * StatsFragmentTest
 * Sergio & kevin
 */
@RunWith(MockitoJUnitRunner.class)
public class SeriesGuidePreferencesTest {

    public static final String KEY_LANGUAGE_PREFERRED = "language";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    SeriesGuidePreferences seriesGuidePreferences;

    @Mock
    Bundle savedInstanceState;

    @Mock
    SharedPreferences preferenceManager;

    @Test
    public void languageChangeTest() {

        ActionBar actionBar;

        //Given
        final String LANGUAGE_FR = "fr";
        preferenceManager.edit().putString(KEY_LANGUAGE_PREFERRED, LANGUAGE_FR).commit();

        //When
        seriesGuidePreferences.onCreate(savedInstanceState);

        //Then
        actionBar = seriesGuidePreferences.getActionBar();
        String test = (String) actionBar.getTitle();
        System.out.println(test);


    }

}