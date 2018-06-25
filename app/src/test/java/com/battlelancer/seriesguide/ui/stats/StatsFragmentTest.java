package com.battlelancer.seriesguide.ui.stats;

import android.os.Bundle;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

/**
 * StatsFragmentTest
 * Sergio & Kevin
 */
@RunWith(MockitoJUnitRunner.class)
public class StatsFragmentTest {

    private StatsLiveData.Stats statsLiveData = new StatsLiveData.Stats();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Bundle savedInstanceState;

    @Mock
    private StatsFragment statsFragment;

    @Test
    public void positiveEpisodesAndMoviesTest() {
        //Given
        statsLiveData.setEpisodesWatched(4);
        statsLiveData.setMoviesWatchlist(5);

        //When
        statsFragment.onActivityCreated(savedInstanceState);

        //Then
        Assert.assertEquals(4, statsLiveData.getEpisodesWatched());
        Assert.assertEquals(5, statsLiveData.getMoviesWatchlist());
    }
}