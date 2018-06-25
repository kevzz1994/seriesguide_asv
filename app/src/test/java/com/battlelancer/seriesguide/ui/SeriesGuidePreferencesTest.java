package com.battlelancer.seriesguide.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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


    public static final String KEY_LANGUAGE_PREFERRED = "language";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    Context context;

    @Mock
    SharedPreferences.Editor editor;

    @Mock
    PreferenceManager preferenceManager;

    @Mock
    SharedPreferences sharedPreferences;

    @Before
    public void setup() {
        context = mock(Context.class);
        preferenceManager = mock(PreferenceManager.class);
        editor = mock(SharedPreferences.Editor.class);
        sharedPreferences = mock(SharedPreferences.class);
        when(preferenceManager.getSharedPreferences().getString(anyString(), anyString())).thenReturn(anyString());
        when(preferenceManager.getSharedPreferences().edit()).thenReturn(editor);
        when(preferenceManager.getSharedPreferences().edit().putString(anyString(), anyString())).thenReturn(editor);
    }
    @Test
    public void languageChangeTest() {

        final String DEFAULT_LANGUAGE = "en";
        final String LANGUAGE_FR = "fr";
        final String LANGUAGE_DE = "de";

        preferenceManager.getSharedPreferences().edit().putString(KEY_LANGUAGE_PREFERRED, LANGUAGE_FR).apply();
        String frenchLanguage = preferenceManager.getSharedPreferences().getString(KEY_LANGUAGE_PREFERRED, DEFAULT_LANGUAGE);

        preferenceManager.getSharedPreferences().edit().putString(KEY_LANGUAGE_PREFERRED, LANGUAGE_DE).apply();
        String deutchLanguage = preferenceManager.getSharedPreferences().getString(KEY_LANGUAGE_PREFERRED, DEFAULT_LANGUAGE);

        System.out.println(frenchLanguage + deutchLanguage);
    }

}