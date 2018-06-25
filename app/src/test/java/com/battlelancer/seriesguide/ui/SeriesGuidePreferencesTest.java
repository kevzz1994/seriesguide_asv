package com.battlelancer.seriesguide.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * StatsFragmentTest
 * Sergio & kevin
 */
@RunWith(MockitoJUnitRunner.class)
public class SeriesGuidePreferencesTest {

    private static final String KEY_LANGUAGE_PREFERRED = "language";

    private static final String STRING_DEFAULT = "TestString";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private PreferenceManager preferenceManager;

    @Mock
    private SharedPreferences sharedPreferences;

    @Before
    public void setup() {
        preferenceManager = mock(PreferenceManager.class);
        editor = mock(SharedPreferences.Editor.class);
        sharedPreferences = mock(SharedPreferences.class);

        when(preferenceManager.getSharedPreferences()).thenReturn(sharedPreferences);
        when(preferenceManager.getSharedPreferences().edit()).thenReturn(editor);
        when(preferenceManager.getSharedPreferences().edit().putString(anyString(), anyString())).thenReturn(editor);
        when(preferenceManager.getSharedPreferences().getString(anyString(), anyString())).thenReturn(STRING_DEFAULT);
    }
    @Test
    public void languageChangeTest() {
        final String DEFAULT_LANGUAGE = "en";
        final String LANGUAGE_FR = "fr";

        preferenceManager.getSharedPreferences().edit().putString(KEY_LANGUAGE_PREFERRED, LANGUAGE_FR).apply();
        String frenchLanguage = preferenceManager.getSharedPreferences().getString(KEY_LANGUAGE_PREFERRED, DEFAULT_LANGUAGE);

        Assert.assertTrue(frenchLanguage.equals(STRING_DEFAULT));
    }
}