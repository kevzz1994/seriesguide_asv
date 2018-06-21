package com.battlelancer.seriesguide.ui;

import android.widget.AutoCompleteTextView;

import com.battlelancer.seriesguide.ui.movies.MoviesSearchActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchMoviesTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testSearch() {
        MoviesSearchActivity moviesSearchActivity = mock(MoviesSearchActivity.class);
        AutoCompleteTextView autoCompleteTextView = mock(AutoCompleteTextView.class);

        when(moviesSearchActivity.getSearchView()).thenReturn(autoCompleteTextView);

        moviesSearchActivity.getSearchView().setText(anyString());
        moviesSearchActivity.search();

        verify(moviesSearchActivity).search();
    }
}
