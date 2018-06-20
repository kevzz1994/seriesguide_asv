
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Season {

    @SerializedName("tvdb_id")
    public int tvdbId;

    private int season;

    private List<Episode> episodes;

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }

    public ContentValues toContentValues(int showTvdbId) {
        ContentValues values = new ContentValues();
        values.put(Seasons._ID, tvdbId);
        values.put(Shows.REF_SHOW_ID, showTvdbId);
        values.put(Seasons.COMBINED, season >= 0 ? season : 0);
        return values;
    }

}
