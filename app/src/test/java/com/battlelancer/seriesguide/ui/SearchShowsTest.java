package com.battlelancer.seriesguide.ui;

import android.widget.AutoCompleteTextView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SearchShowsTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testSearch() {
        SearchActivity searchActivity = mock(SearchActivity.class);
        AutoCompleteTextView autoCompleteTextView = mock(AutoCompleteTextView.class);

        when(searchActivity.getSearchView()).thenReturn(autoCompleteTextView);

        searchActivity.getSearchView().setText(anyString());
        searchActivity.triggerTvdbSearch();

        verify(searchActivity).triggerTvdbSearch();
    }
}
