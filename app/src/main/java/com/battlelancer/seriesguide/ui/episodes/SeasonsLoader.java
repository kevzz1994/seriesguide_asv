package com.battlelancer.seriesguide.ui.episodes;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads show and season info based on a given episode.
 */
class SeasonsLoader extends GenericSimpleLoader<SeasonsLoader.Result> {

    private final int episodeTvdbId;

    static class Result {
        private int showTvdbId;
        private String showTitle;
        private String showPoster;
        private int seasonIndexOfEpisode;
        @NonNull
        private List<Season> seasonsOfShow;

        public Result(int showTvdbId, String showTitle, String showPoster,
                int seasonIndexOfEpisode, @NonNull List<Season> seasonsOfShow) {
            this.showTvdbId = showTvdbId;
            this.showTitle = showTitle;
            this.showPoster = showPoster;
            this.seasonIndexOfEpisode = seasonIndexOfEpisode;
            this.seasonsOfShow = seasonsOfShow;
        }

        public int getShowTvdbId() {
            return showTvdbId;
        }

        public void setShowTvdbId(int showTvdbId) {
            this.showTvdbId = showTvdbId;
        }

        public String getShowTitle() {
            return showTitle;
        }

        public void setShowTitle(String showTitle) {
            this.showTitle = showTitle;
        }

        public String getShowPoster() {
            return showPoster;
        }

        public void setShowPoster(String showPoster) {
            this.showPoster = showPoster;
        }

        public int getSeasonIndexOfEpisode() {
            return seasonIndexOfEpisode;
        }

        public void setSeasonIndexOfEpisode(int seasonIndexOfEpisode) {
            this.seasonIndexOfEpisode = seasonIndexOfEpisode;
        }

        @NonNull
        public List<Season> getSeasonsOfShow() {
            return seasonsOfShow;
        }

        public void setSeasonsOfShow(@NonNull List<Season> seasonsOfShow) {
            this.seasonsOfShow = seasonsOfShow;
        }
    }

    SeasonsLoader(Context context, int episodeTvdbId) {
        super(context);
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    public Result loadInBackground() {
        // query for show and season info of the given episode
        Cursor episodeQuery = getContext().getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeWithShowUri(String.valueOf(episodeTvdbId)),
                EpisodeQuery.PROJECTION, null, null, null);
        if (episodeQuery == null) {
            return null;
        }
        if (!episodeQuery.moveToFirst()) {
            episodeQuery.close();
            return null;
        }

        int showTvdbId = episodeQuery.getInt(EpisodeQuery.SHOW_TVDB_ID);
        String showTitle = episodeQuery.getString(EpisodeQuery.SHOW_TITLE);
        String showPoster = episodeQuery.getString(EpisodeQuery.SHOW_POSTER);
        int seasonTvdbId = episodeQuery.getInt(EpisodeQuery.SEASON_TVDB_ID);

        episodeQuery.close();

        // query for seasons of the show
        List<Season> seasons = new ArrayList<>();
        Cursor seasonsQuery = getContext().getContentResolver()
                .query(SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId),
                        SeasonsQuery.PROJECTION, null, null,
                        DisplaySettings.getSeasonSortOrder(getContext()).query());
        if (seasonsQuery == null) {
            return null;
        }
        int seasonIndex = 0;
        int seasonIndexOfEpisode = 0;
        while (seasonsQuery.moveToNext()) {
            Season season = new Season();
            season.tvdbId = seasonsQuery.getInt(SeasonsQuery.ID);
            season.setSeason(seasonsQuery.getInt(SeasonsQuery.NUMBER));
            seasons.add(season);

            if (season.tvdbId == seasonTvdbId) {
                seasonIndexOfEpisode = seasonIndex;
            }
            seasonIndex++;
        }
        seasonsQuery.close();

        return new Result(showTvdbId, showTitle, showPoster, seasonIndexOfEpisode, seasons);
    }

    static class EpisodeQuery {
        static String[] PROJECTION = new String[] {
                SeriesGuideContract.Shows.REF_SHOW_ID,
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Seasons.REF_SEASON_ID,
        };
        static int SHOW_TVDB_ID = 0;
        static int SHOW_TITLE = 1;
        static int SHOW_POSTER = 2;
        static int SEASON_TVDB_ID = 3;
    }

    static class SeasonsQuery {
        static String[] PROJECTION = new String[] {
                SeriesGuideContract.Seasons._ID,
                SeriesGuideContract.Seasons.COMBINED
        };
        static int ID = 0;
        static int NUMBER = 1;
    }
}
