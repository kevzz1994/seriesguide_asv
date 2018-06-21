package com.battlelancer.seriesguide.ui;

import android.content.Context;

import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

@RunWith(MockitoJUnitRunner.class)
public class EpisodeTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EpisodeWatchedJob episodeWatchedJob;

    @Before
    public void setup() {
        episodeWatchedJob = new EpisodeWatchedJob(1, 1, 1, 1,
                EpisodeFlags.WATCHED);
    }

    @Test
    public void testEpisodeWatched() {
        Assert.assertTrue(EpisodeTools.isWatched(episodeWatchedJob.getFlag()));
    }

}
