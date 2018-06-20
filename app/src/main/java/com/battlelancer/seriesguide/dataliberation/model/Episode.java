
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.google.gson.annotations.SerializedName;

public class Episode {

    @SerializedName("tvdb_id")
    public int tvdbId;

    private int episode;

    @SerializedName("episode_absolute")
    public int episodeAbsolute;

    private String title;

    @SerializedName("first_aired")
    public long firstAired;

    private boolean watched;

    private boolean skipped;

    private boolean collected;

    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * Full dump only follows.
     */

    @SerializedName("episode_dvd")
    public double episodeDvd;

    private String overview;

    private String image;

    private String writers;

    private String gueststars;

    private String directors;

    private double rating;
    private int rating_votes;
    private int rating_user;

    @SerializedName("last_edited")
    public long lastEdited;

    public int getTvdbId() {
        return tvdbId;
    }

    public int getEpisode() {
        return episode;
    }

    public int getEpisodeAbsolute() {
        return episodeAbsolute;
    }

    public String getTitle() {
        return title;
    }

    public long getFirstAired() {
        return firstAired;
    }

    public boolean isWatched() {
        return watched;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isCollected() {
        return collected;
    }

    public String getImdbId() {
        return imdbId;
    }

    public double getEpisodeDvd() {
        return episodeDvd;
    }

    public String getOverview() {
        return overview;
    }

    public String getImage() {
        return image;
    }

    public String getWriters() {
        return writers;
    }

    public String getGueststars() {
        return gueststars;
    }

    public String getDirectors() {
        return directors;
    }

    public double getRating() {
        return rating;
    }

    public int getRating_votes() {
        return rating_votes;
    }

    public int getRating_user() {
        return rating_user;
    }

    public long getLastEdited() {
        return lastEdited;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public void setEpisodeAbsolute(int episodeAbsolute) {
        this.episodeAbsolute = episodeAbsolute;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFirstAired(long firstAired) {
        this.firstAired = firstAired;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public void setEpisodeDvd(double episodeDvd) {
        this.episodeDvd = episodeDvd;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setWriters(String writers) {
        this.writers = writers;
    }

    public void setGueststars(String gueststars) {
        this.gueststars = gueststars;
    }

    public void setDirectors(String directors) {
        this.directors = directors;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setRating_votes(int rating_votes) {
        this.rating_votes = rating_votes;
    }

    public void setRating_user(int rating_user) {
        this.rating_user = rating_user;
    }

    public void setLastEdited(long lastEdited) {
        this.lastEdited = lastEdited;
    }

    public ContentValues toContentValues(int showTvdbId, int seasonTvdbId, int seasonNumber) {
        ContentValues values = new ContentValues();
        values.put(Episodes._ID, tvdbId);

        values.put(Episodes.TITLE, title != null ? title : "");
        values.put(Episodes.OVERVIEW, overview);
        values.put(Episodes.NUMBER, episode >= 0 ? episode : 0);
        values.put(Episodes.SEASON, seasonNumber);
        values.put(Episodes.DVDNUMBER, episodeDvd >= 0 ? episodeDvd : 0);

        values.put(Shows.REF_SHOW_ID, showTvdbId);
        values.put(Seasons.REF_SEASON_ID, seasonTvdbId);

        // watched/skipped represented internally in watched flag
        values.put(Episodes.WATCHED, skipped
                ? EpisodeFlags.SKIPPED : watched
                ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);

        values.put(Episodes.DIRECTORS, directors != null ? directors : "");
        values.put(Episodes.GUESTSTARS, gueststars != null ? gueststars : "");
        values.put(Episodes.WRITERS, writers != null ? writers : "");
        values.put(Episodes.IMAGE, image != null ? image : "");

        values.put(Episodes.FIRSTAIREDMS, firstAired);
        values.put(Episodes.COLLECTED, collected);

        values.put(Episodes.RATING_GLOBAL, (rating >= 0 && rating <= 10) ? rating : 0);
        values.put(Episodes.RATING_VOTES, rating_votes >= 0 ? rating_votes : 0);
        values.put(Episodes.RATING_USER,
                (rating_user >= 0 && rating_user <= 10) ? rating_user : 0);

        values.put(Episodes.IMDBID, imdbId != null ? imdbId : "");
        values.put(Episodes.LAST_EDITED, lastEdited);
        values.put(Episodes.ABSOLUTE_NUMBER, episodeAbsolute >= 0 ? episodeAbsolute : 0);

        return values;
    }


}
