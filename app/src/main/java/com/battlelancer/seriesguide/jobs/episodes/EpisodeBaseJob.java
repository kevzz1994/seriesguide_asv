package com.battlelancer.seriesguide.jobs.episodes;

import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Flagging single episodes watched or collected.
 */
public abstract class EpisodeBaseJob extends BaseEpisodesJob {

    protected int episodeTvdbId;
    protected int season;
    protected int episode;

    public EpisodeBaseJob(int showTvdbId, int episodeTvdbId, int season, int episode, int flagValue,
            JobAction action) {
        super(showTvdbId, flagValue, action);
        this.episodeTvdbId = episodeTvdbId;
        this.season = season;
        this.episode = episode;
    }

    public int getEpisodeTvdbId() {
        return episodeTvdbId;
    }

    public void setEpisodeTvdbId(int episodeTvdbId) {
        this.episodeTvdbId = episodeTvdbId;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public int getFlag() {
        return super.getFlagValue();
    }

    @Override
    public Uri getDatabaseUri() {
        return SeriesGuideContract.Episodes.buildEpisodeUri(String.valueOf(episodeTvdbId));
    }

    @Override
    public String getDatabaseSelection() {
        return null;
    }
}
