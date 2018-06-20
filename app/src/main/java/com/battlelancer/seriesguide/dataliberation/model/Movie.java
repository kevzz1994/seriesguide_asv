package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.util.DBUtils;
import com.google.gson.annotations.SerializedName;

public class Movie {

    @SerializedName("tmdb_id")
    public int tmdbId;

    @SerializedName("imdb_id")
    public String imdbId;

    private String title;

    @SerializedName("released_utc_ms")
    public long releasedUtcMs;

    @SerializedName("runtime_min")
    public int runtimeMin;

    private String poster;

    private String overview;

    @SerializedName("in_collection")
    public boolean inCollection;

    @SerializedName("in_watchlist")
    public boolean inWatchlist;

    private boolean watched;

    public int getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getReleasedUtcMs() {
        return releasedUtcMs;
    }

    public void setReleasedUtcMs(long releasedUtcMs) {
        this.releasedUtcMs = releasedUtcMs;
    }

    public int getRuntimeMin() {
        return runtimeMin;
    }

    public void setRuntimeMin(int runtimeMin) {
        this.runtimeMin = runtimeMin;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public boolean isInCollection() {
        return inCollection;
    }

    public void setInCollection(boolean inCollection) {
        this.inCollection = inCollection;
    }

    public boolean isInWatchlist() {
        return inWatchlist;
    }

    public void setInWatchlist(boolean inWatchlist) {
        this.inWatchlist = inWatchlist;
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Movies.TMDB_ID, tmdbId);
        values.put(Movies.IMDB_ID, imdbId);
        values.put(Movies.TITLE, title);
        values.put(Movies.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(title));
        values.put(Movies.RELEASED_UTC_MS, releasedUtcMs);
        values.put(Movies.RUNTIME_MIN, runtimeMin);
        values.put(Movies.POSTER, poster);
        values.put(Movies.IN_COLLECTION, inCollection);
        values.put(Movies.IN_WATCHLIST, inWatchlist);
        values.put(Movies.WATCHED, watched);
        // full dump values
        values.put(Movies.OVERVIEW, overview);
        return values;
    }

}
