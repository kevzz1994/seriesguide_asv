
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.List;

/**
 * @see com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns ShowsColumns
 */
public class Show {

    private int tvdb_id;
    private String imdb_id;
    private Integer trakt_id;

    private String title;
    private String overview;

    private String language;

    private String first_aired;
    private int release_time;
    private int release_weekday;
    private String release_timezone;
    private String country;

    private String poster;
    private String content_rating;
    private String status;
    private int runtime;
    private String genres;
    private String network;

    private double rating;
    private int rating_votes;
    private int rating_user;

    private long last_edited;

    /** SeriesGuide specific values */
    private boolean favorite;
    private Boolean notify;
    private boolean hidden;

    private long last_updated;
    private int last_watched_episode;
    private long last_watched_ms;

    private List<Season> seasons;

    public int getTvdb_id() {
        return tvdb_id;
    }

    public void setTvdb_id(int tvdb_id) {
        this.tvdb_id = tvdb_id;
    }

    public String getImdb_id() {
        return imdb_id;
    }

    public void setImdb_id(String imdb_id) {
        this.imdb_id = imdb_id;
    }

    public Integer getTrakt_id() {
        return trakt_id;
    }

    public void setTrakt_id(Integer trakt_id) {
        this.trakt_id = trakt_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFirst_aired() {
        return first_aired;
    }

    public void setFirst_aired(String first_aired) {
        this.first_aired = first_aired;
    }

    public int getRelease_time() {
        return release_time;
    }

    public void setRelease_time(int release_time) {
        this.release_time = release_time;
    }

    public int getRelease_weekday() {
        return release_weekday;
    }

    public void setRelease_weekday(int release_weekday) {
        this.release_weekday = release_weekday;
    }

    public String getRelease_timezone() {
        return release_timezone;
    }

    public void setRelease_timezone(String release_timezone) {
        this.release_timezone = release_timezone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getContent_rating() {
        return content_rating;
    }

    public void setContent_rating(String content_rating) {
        this.content_rating = content_rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRating_votes() {
        return rating_votes;
    }

    public void setRating_votes(int rating_votes) {
        this.rating_votes = rating_votes;
    }

    public int getRating_user() {
        return rating_user;
    }

    public void setRating_user(int rating_user) {
        this.rating_user = rating_user;
    }

    public long getLast_edited() {
        return last_edited;
    }

    public void setLast_edited(long last_edited) {
        this.last_edited = last_edited;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public Boolean getNotify() {
        return notify;
    }

    public void setNotify(Boolean notify) {
        this.notify = notify;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public long getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(long last_updated) {
        this.last_updated = last_updated;
    }

    public int getLast_watched_episode() {
        return last_watched_episode;
    }

    public void setLast_watched_episode(int last_watched_episode) {
        this.last_watched_episode = last_watched_episode;
    }

    public long getLast_watched_ms() {
        return last_watched_ms;
    }

    public void setLast_watched_ms(long last_watched_ms) {
        this.last_watched_ms = last_watched_ms;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }

    public ContentValues toContentValues(Context context, boolean forInsert) {
        // note: if a value is explicitly inserted as NULL the DEFAULT value is not used
        // so ensure a NULL is never inserted if a DEFAULT constraint exists

        ContentValues values = new ContentValues();
        // values for new and existing shows
        // if in any case the title is empty, show a place holder
        values.put(Shows.TITLE, TextUtils.isEmpty(title) 
                ? context.getString(R.string.no_translation_title) : title);
        values.put(Shows.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(title));
        values.put(Shows.OVERVIEW, overview != null ? overview : "");
        values.put(Shows.POSTER, poster != null ? poster : "");
        values.put(Shows.CONTENTRATING, content_rating != null ? content_rating : "");
        values.put(Shows.STATUS, DataLiberationTools.encodeShowStatus(status));
        values.put(Shows.RUNTIME, runtime >= 0 ? runtime : 0);
        values.put(Shows.RATING_GLOBAL, (rating >= 0 && rating <= 10) ? rating : 0);
        values.put(Shows.NETWORK, network != null ? network : "");
        values.put(Shows.GENRES, genres != null ? genres : "");
        values.put(Shows.FIRST_RELEASE, first_aired);
        values.put(Shows.RELEASE_TIME, release_time);
        values.put(Shows.RELEASE_WEEKDAY, (release_weekday >= -1 && release_weekday <= 7)
                ? release_weekday : TimeTools.RELEASE_WEEKDAY_UNKNOWN);
        values.put(Shows.RELEASE_TIMEZONE, release_timezone);
        values.put(Shows.RELEASE_COUNTRY, country);
        values.put(Shows.IMDBID, imdb_id != null ? imdb_id : "");
        values.put(Shows.TRAKT_ID, (trakt_id != null && trakt_id > 0) ? trakt_id : 0);
        values.put(Shows.LASTUPDATED, last_updated);
        values.put(Shows.LASTEDIT, last_edited);
        if (forInsert) {
            values.put(Shows._ID, tvdb_id);
            values.put(Shows.LANGUAGE, language != null ? language : DisplaySettings.LANGUAGE_EN);

            values.put(Shows.FAVORITE, favorite);
            values.put(Shows.NOTIFY, notify != null ? notify : true);
            values.put(Shows.HIDDEN, hidden);

            values.put(Shows.RATING_VOTES, rating_votes >= 0 ? rating_votes : 0);
            values.put(Shows.RATING_USER, (rating_user >= 0 && rating_user <= 10)
                    ? rating_user : 0);

            values.put(Shows.LASTWATCHEDID, last_watched_episode);
            values.put(Shows.LASTWATCHED_MS, last_watched_ms);

            values.put(Shows.HEXAGON_MERGE_COMPLETE, 1);
            values.put(Shows.NEXTEPISODE, "");
            values.put(Shows.NEXTTEXT, "");
            values.put(Shows.NEXTAIRDATEMS, DBUtils.UNKNOWN_NEXT_RELEASE_DATE);
            values.put(Shows.UNWATCHED_COUNT, DBUtils.UNKNOWN_UNWATCHED_COUNT);
        }
        return values;
    }
}
