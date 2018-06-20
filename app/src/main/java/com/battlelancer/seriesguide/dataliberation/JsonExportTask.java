package com.battlelancer.seriesguide.dataliberation;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import com.uwetrottmann.androidutils.AndroidUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

/**
 * Export the show database to a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 */
public class JsonExportTask extends AsyncTask<Void, Integer, Integer> {

    public interface OnTaskProgressListener {
        void onProgressUpdate(Integer... values);
    }

    public static final String EXPORT_FOLDER = "SeriesGuide";
    public static final String EXPORT_FOLDER_AUTO = "SeriesGuide" + File.separator + "AutoBackup";
    public static final String EXPORT_JSON_FILE_SHOWS = "sg-shows-export.json";
    public static final String EXPORT_JSON_FILE_LISTS = "sg-lists-export.json";
    public static final String EXPORT_JSON_FILE_MOVIES = "sg-movies-export.json";

    public static final int BACKUP_SHOWS = 1;
    public static final int BACKUP_LISTS = 2;
    public static final int BACKUP_MOVIES = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_SHOWS,
            BACKUP_LISTS,
            BACKUP_MOVIES
    })
    public @interface BackupType {
    }

    private static final int SUCCESS = 1;
    private static final int ERROR_FILE_ACCESS = 0;
    private static final int ERROR = -1;

    /**
     * Show status used when exporting data. Compare with {@link ShowTools.Status}.
     */
    public interface ShowStatusExport {
        String CONTINUING = "continuing";
        String ENDED = "ended";
        String UNKNOWN = "unknown";
    }

    public interface ListItemTypesExport {
        String SHOW = "show";
        String SEASON = "season";
        String EPISODE = "episode";
    }

    @SuppressLint("StaticFieldLeak")
    private Context context;
    private OnTaskProgressListener progressListener;
    private boolean isFullDump;
    private boolean isAutoBackupMode;
    @Nullable
    private final Integer type;
    private boolean isUseDefaultFolders;
    @Nullable
    private String errorCause;

    public static File getExportPath(boolean isAutoBackupMode) {
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                isAutoBackupMode ? EXPORT_FOLDER_AUTO : EXPORT_FOLDER);
    }

    /**
     * Same as {@link JsonExportTask} but allows to set parameters.
     *
     * @param isFullDump       Whether to also export meta-data like descriptions, ratings, actors, etc.
     *                         Increases file size about 2-4 times.
     * @param isAutoBackupMode Whether to run an auto backup, also shows no result toasts.
     */
    public JsonExportTask(Context context, OnTaskProgressListener progressListener,
                          boolean isFullDump, boolean isAutoBackupMode, @Nullable Integer type) {
        this.context = context.getApplicationContext();
        this.progressListener = progressListener;
        this.isFullDump = isFullDump;
        this.isAutoBackupMode = isAutoBackupMode;
        this.type = type;
        // use Storage Access Framework on KitKat and up to select custom backup files,
        // on older versions use default folders
        // also auto backup by default uses default folders
        isUseDefaultFolders = !AndroidUtils.isKitKatOrHigher()
                || (isAutoBackupMode && BackupSettings.isUseAutoBackupDefaultFiles(context));
    }

    @Override
    protected Integer doInBackground(Void... params) {
        File exportPath = null;
        if (isUseDefaultFolders) {
            // Ensure external storage is available
            if (!AndroidUtils.isExtStorageAvailable()) {
                return ERROR_FILE_ACCESS;
            }

            // Ensure the export directory exists
            exportPath = getExportPath(isAutoBackupMode);
            if (!exportPath.mkdirs() && !exportPath.isDirectory()) {
                return ERROR_FILE_ACCESS;
            }
        }

        // last chance to abort
        if (isCancelled()) {
            return ERROR;
        }

        int result;
        if (type == null || type == BACKUP_SHOWS) {
            result = exportData(exportPath, BACKUP_SHOWS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (type == null || type == BACKUP_LISTS) {
            result = exportData(exportPath, BACKUP_LISTS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (type == null || type == BACKUP_MOVIES) {
            result = exportData(exportPath, BACKUP_MOVIES);
            if (result != SUCCESS) {
                return result;
            }
        }
        // no need to return early here if canceled, we are almost done anyhow

        if (isAutoBackupMode) {
            // store current time = last backup time
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit()
                    .putLong(AdvancedSettings.KEY_LASTBACKUP, System.currentTimeMillis())
                    .apply();
        }

        return SUCCESS;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (progressListener != null) {
            progressListener.onProgressUpdate(values);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        TaskManager.getInstance().releaseBackupTaskRef();

        if (!isAutoBackupMode) {
            int messageId;
            boolean showIndefinite;
            switch (result) {
                case SUCCESS:
                    messageId = R.string.backup_success;
                    showIndefinite = false;
                    break;
                case ERROR_FILE_ACCESS:
                    messageId = R.string.backup_failed_file_access;
                    showIndefinite = true;
                    break;
                default:
                    messageId = R.string.backup_failed;
                    showIndefinite = true;
                    break;
            }
            EventBus.getDefault()
                    .post(new DataLiberationFragment.LiberationResultEvent(
                            context.getString(messageId), errorCause, showIndefinite));
        } else {
            EventBus.getDefault().post(new DataLiberationFragment.LiberationResultEvent());
        }
    }

    private int exportData(File exportPath, @BackupType int type) {
        // check if there is any data to export
        Cursor data = getDataCursor(type);
        if (data == null) {
            // query failed
            return ERROR;
        }
        if (data.getCount() == 0) {
            // There is no data? Done.
            data.close();
            return SUCCESS;
        }

        publishProgress(data.getCount(), 0);

        // try to export all data
        try {
            if (!isUseDefaultFolders) {
                // ensure the user has selected a backup file
                Uri backupFileUri = getDataBackupFile(type);
                if (backupFileUri == null) {
                    return ERROR_FILE_ACCESS;
                }

                try (ParcelFileDescriptor pfd = context.getContentResolver()
                        .openFileDescriptor(backupFileUri, "w")) {
                    if (pfd == null) {
                        return ERROR_FILE_ACCESS;
                    }

                    try (FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor())) {
                        if (type == BACKUP_SHOWS) {
                            writeJsonStreamShows(out, data);
                        } else if (type == BACKUP_LISTS) {
                            writeJsonStreamLists(out, data);
                        } else if (type == BACKUP_MOVIES) {
                            writeJsonStreamMovies(out, data);
                        }
                    }
                }
            } else {
                File backupFile;
                if (type == BACKUP_SHOWS) {
                    backupFile = new File(exportPath, EXPORT_JSON_FILE_SHOWS);
                } else if (type == BACKUP_LISTS) {
                    backupFile = new File(exportPath, EXPORT_JSON_FILE_LISTS);
                } else if (type == BACKUP_MOVIES) {
                    backupFile = new File(exportPath, EXPORT_JSON_FILE_MOVIES);
                } else {
                    return ERROR;
                }

                OutputStream out = new FileOutputStream(backupFile);
                if (type == BACKUP_SHOWS) {
                    writeJsonStreamShows(out, data);
                } else if (type == BACKUP_LISTS) {
                    writeJsonStreamLists(out, data);
                } else {
                    writeJsonStreamMovies(out, data);
                }
            }
        } catch (FileNotFoundException e) {
            Timber.e(e, "Backup file not found.");
            removeBackupFileUri(type);
            errorCause = e.getMessage();
            return ERROR_FILE_ACCESS;
        } catch (IOException | SecurityException e) {
            Timber.e(e, "Could not access backup file.");
            removeBackupFileUri(type);
            errorCause = e.getMessage();
            return ERROR_FILE_ACCESS;
        } catch (JsonParseException e) {
            Timber.e(e, "JSON export failed.");
            errorCause = e.getMessage();
            return ERROR;
        } finally {
            data.close();
        }

        return SUCCESS;
    }

    @Nullable
    private Cursor getDataCursor(@BackupType int type) {
        if (type == BACKUP_SHOWS) {
            return context.getContentResolver().query(
                    Shows.CONTENT_URI,
                    isFullDump ? ShowsQuery.PROJECTION_FULL : ShowsQuery.PROJECTION,
                    null, null, Shows.SORT_TITLE);
        }
        if (type == BACKUP_LISTS) {
            return context.getContentResolver()
                    .query(SeriesGuideContract.Lists.CONTENT_URI,
                            ListsQuery.PROJECTION, null, null,
                            SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        }
        if (type == BACKUP_MOVIES) {
            return context.getContentResolver()
                    .query(Movies.CONTENT_URI,
                            MoviesQuery.PROJECTION, null, null, MoviesQuery.SORT_ORDER);
        }
        return null;
    }

    @Nullable
    private Uri getDataBackupFile(@BackupType int type) {
        if (type == BACKUP_SHOWS) {
            return BackupSettings.getFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI
                            : BackupSettings.KEY_SHOWS_EXPORT_URI);
        }
        if (type == BACKUP_LISTS) {
            return BackupSettings.getFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI
                            : BackupSettings.KEY_LISTS_EXPORT_URI);
        }
        if (type == BACKUP_MOVIES) {
            return BackupSettings.getFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI
                            : BackupSettings.KEY_MOVIES_EXPORT_URI);
        }
        return null;
    }

    private void removeBackupFileUri(@BackupType int type) {
        if (type == BACKUP_SHOWS) {
            BackupSettings.storeFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI
                            : BackupSettings.KEY_SHOWS_EXPORT_URI, null);
        } else if (type == BACKUP_LISTS) {
            BackupSettings.storeFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI
                            : BackupSettings.KEY_LISTS_EXPORT_URI, null);
        } else if (type == BACKUP_MOVIES) {
            BackupSettings.storeFileUri(context,
                    isAutoBackupMode ? BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI
                            : BackupSettings.KEY_MOVIES_EXPORT_URI, null);
        }
    }

    private void writeJsonStreamShows(OutputStream out, Cursor shows) throws IOException {
        int numTotal = shows.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.beginArray();

        while (shows.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            Show show = new Show();
            show.setTvdb_id(shows.getInt(ShowsQuery.ID));
            show.setTitle(shows.getString(ShowsQuery.TITLE));
            show.setFavorite(shows.getInt(ShowsQuery.FAVORITE) == 1);
            show.setNotify(shows.getInt(ShowsQuery.NOTIFY) == 1);
            show.setHidden(shows.getInt(ShowsQuery.HIDDEN) == 1);
            show.setLanguage(shows.getString(ShowsQuery.LANGUAGE));
            show.setRelease_time(shows.getInt(ShowsQuery.RELEASE_TIME));
            show.setRelease_weekday(shows.getInt(ShowsQuery.RELEASE_WEEKDAY));
            show.setRelease_timezone(shows.getString(ShowsQuery.RELEASE_TIMEZONE));
            show.setCountry(shows.getString(ShowsQuery.RELEASE_COUNTRY));
            show.setLast_watched_episode(shows.getInt(ShowsQuery.LASTWATCHEDID));
            show.setLast_watched_ms(shows.getLong(ShowsQuery.LASTWATCHED_MS));
            show.setPoster(shows.getString(ShowsQuery.POSTER));
            show.setContent_rating(shows.getString(ShowsQuery.CONTENTRATING));
            show.setStatus(DataLiberationTools.decodeShowStatus(shows.getInt(ShowsQuery.STATUS)));
            show.setRuntime(shows.getInt(ShowsQuery.RUNTIME));
            show.setNetwork(shows.getString(ShowsQuery.NETWORK));
            show.setImdb_id(shows.getString(ShowsQuery.IMDBID));
            show.setTrakt_id(shows.getInt(ShowsQuery.TRAKT_ID));
            show.setFirst_aired(shows.getString(ShowsQuery.FIRSTAIRED));
            show.setRating_user(shows.getInt(ShowsQuery.RATING_USER));
            if (isFullDump) {
                show.setOverview(shows.getString(ShowsQuery.OVERVIEW));
                show.setRating(shows.getDouble(ShowsQuery.RATING_GLOBAL));
                show.setRating_votes(shows.getInt(ShowsQuery.RATING_VOTES));
                show.setGenres(shows.getString(ShowsQuery.GENRES));
                show.setLast_updated(shows.getLong(ShowsQuery.LAST_UPDATED));
                show.setLast_edited(shows.getLong(ShowsQuery.LAST_EDITED));
            }

            addSeasons(show);

            gson.toJson(show, Show.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addSeasons(Show show) {
        ArrayList<Season> seasonsList = new ArrayList<>();
        show.setSeasons(seasonsList);
        final Cursor seasonsCursor = context.getContentResolver().query(
                Seasons.buildSeasonsOfShowUri(String.valueOf(show.getTvdb_id())),
                new String[]{
                        Seasons._ID,
                        Seasons.COMBINED
                }, null, null, null
        );

        if (seasonsCursor == null) {
            return;
        }

        while (seasonsCursor.moveToNext()) {
            Season season = new Season();
            season.tvdbId = seasonsCursor.getInt(0);
            season.setSeason(seasonsCursor.getInt(1));

            addEpisodes(season);

            show.getSeasons().add(season);
        }

        seasonsCursor.close();
    }

    private void addEpisodes(Season season) {
        ArrayList<Episode> episodeList = new ArrayList<>();
        season.setEpisodes(episodeList);
        final Cursor episodesCursor = context.getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(season.tvdbId)),
                isFullDump ? EpisodesQuery.PROJECTION_FULL : EpisodesQuery.PROJECTION, null, null,
                EpisodesQuery.SORT);

        if (episodesCursor == null) {
            return;
        }

        while (episodesCursor.moveToNext()) {
            Episode episode = new Episode();
            episode.setTvdbId(episodesCursor.getInt(EpisodesQuery.ID));
            episode.setEpisode(episodesCursor.getInt(EpisodesQuery.NUMBER));
            episode.episodeAbsolute = episodesCursor.getInt(EpisodesQuery.NUMBER_ABSOLUTE);
            episode.episodeDvd = episodesCursor.getDouble(EpisodesQuery.NUMBER_DVD);
            int episodeFlag = episodesCursor.getInt(EpisodesQuery.WATCHED);
            episode.setWatched(EpisodeTools.isWatched(episodeFlag));
            episode.setSkipped(EpisodeTools.isSkipped(episodeFlag));
            episode.setCollected(episodesCursor.getInt(EpisodesQuery.COLLECTED) == 1);
            episode.setTitle(episodesCursor.getString(EpisodesQuery.TITLE));
            episode.firstAired = episodesCursor.getLong(EpisodesQuery.FIRSTAIRED);
            episode.imdbId = episodesCursor.getString(EpisodesQuery.IMDBID);
            episode.setRating_user(episodesCursor.getInt(EpisodesQuery.RATING_USER));
            if (isFullDump) {
                episode.setOverview(episodesCursor.getString(EpisodesQuery.OVERVIEW));
                episode.setImage(episodesCursor.getString(EpisodesQuery.IMAGE));
                episode.setWriters(episodesCursor.getString(EpisodesQuery.WRITERS));
                episode.setGueststars(episodesCursor.getString(EpisodesQuery.GUESTSTARS));
                episode.setDirectors(episodesCursor.getString(EpisodesQuery.DIRECTORS));
                episode.setRating(episodesCursor.getDouble(EpisodesQuery.RATING_GLOBAL));
                episode.setRating_votes(episodesCursor.getInt(EpisodesQuery.RATING_VOTES));
                episode.lastEdited = episodesCursor.getLong(EpisodesQuery.LAST_EDITED);
            }

            season.getEpisodes().add(episode);
        }

        episodesCursor.close();
    }

    private void writeJsonStreamLists(OutputStream out, Cursor lists) throws IOException {
        int numTotal = lists.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.beginArray();

        while (lists.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            List list = new List();
            list.listId = lists.getString(ListsQuery.ID);
            list.setName(lists.getString(ListsQuery.NAME));
            list.setOrder(lists.getInt(ListsQuery.ORDER));

            addListItems(list);

            gson.toJson(list, List.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addListItems(List list) {
        final Cursor listItems = context.getContentResolver().query(
                ListItems.CONTENT_URI, ListItemsQuery.PROJECTION,
                ListItemsQuery.SELECTION,
                new String[]{
                        list.listId
                }, null
        );
        if (listItems == null) {
            return;
        }

        //list.items = new ArrayList<>();
        while (listItems.moveToNext()) {
            ListItem item = new ListItem();
            item.listItemId = listItems.getString(ListItemsQuery.ID);
            item.tvdbId = listItems.getInt(ListItemsQuery.ITEM_REF_ID);
            switch (listItems.getInt(ListItemsQuery.TYPE)) {
                case ListItemTypes.SHOW:
                    item.setType( ListItemTypesExport.SHOW);
                    break;
                case ListItemTypes.SEASON:
                    item.setType(ListItemTypesExport.SEASON);
                    break;
                case ListItemTypes.EPISODE:
                    item.setType(ListItemTypesExport.EPISODE);
                    break;
            }

            list.getItems().add(item);
        }

        listItems.close();
    }

    private void writeJsonStreamMovies(OutputStream out, Cursor movies) throws IOException {
        int numTotal = movies.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.beginArray();

        while (movies.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            Movie movie = new Movie();
            movie.tmdbId = movies.getInt(MoviesQuery.TMDB_ID);
            movie.imdbId = movies.getString(MoviesQuery.IMDB_ID);
            movie.setTitle(movies.getString(MoviesQuery.TITLE));
            movie.releasedUtcMs = movies.getLong(MoviesQuery.RELEASED_UTC_MS);
            movie.runtimeMin = movies.getInt(MoviesQuery.RUNTIME_MIN);
            movie.setPoster(movies.getString(MoviesQuery.POSTER));
            movie.inCollection = movies.getInt(MoviesQuery.IN_COLLECTION) == 1;
            movie.inWatchlist = movies.getInt(MoviesQuery.IN_WATCHLIST) == 1;
            movie.setWatched(movies.getInt(MoviesQuery.WATCHED) == 1);
            if (isFullDump) {
                movie.setOverview(movies.getString(MoviesQuery.OVERVIEW));
            }

            gson.toJson(movie, Movie.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    public static class ShowsQuery {
        static String[] PROJECTION = new String[]{
                Shows._ID,
                Shows.TITLE,
                Shows.FAVORITE,
                Shows.NOTIFY,
                Shows.HIDDEN,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.LASTWATCHEDID,
                Shows.LASTWATCHED_MS,
                Shows.POSTER,
                Shows.CONTENTRATING,
                Shows.STATUS,
                Shows.RUNTIME,
                Shows.NETWORK,
                Shows.IMDBID,
                Shows.TRAKT_ID,
                Shows.FIRST_RELEASE,
                Shows.RATING_USER,
                Shows.LANGUAGE
        };
        static String[] PROJECTION_FULL = new String[]{
                Shows._ID,
                Shows.TITLE,
                Shows.FAVORITE,
                Shows.NOTIFY,
                Shows.HIDDEN,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.LASTWATCHEDID,
                Shows.LASTWATCHED_MS,
                Shows.POSTER,
                Shows.CONTENTRATING,
                Shows.STATUS,
                Shows.RUNTIME,
                Shows.NETWORK,
                Shows.IMDBID,
                Shows.TRAKT_ID,
                Shows.FIRST_RELEASE,
                Shows.RATING_USER,
                Shows.LANGUAGE,
                Shows.OVERVIEW,
                Shows.RATING_GLOBAL,
                Shows.RATING_VOTES,
                Shows.GENRES,
                Shows.LASTUPDATED,
                Shows.LASTEDIT
        };

        static int ID = 0;
        static int TITLE = 1;
        static int FAVORITE = 2;
        static int NOTIFY = 3;
        static int HIDDEN = 4;
        static int RELEASE_TIME = 5;
        static int RELEASE_WEEKDAY = 6;
        static int RELEASE_TIMEZONE = 7;
        static int RELEASE_COUNTRY = 8;
        static int LASTWATCHEDID = 9;
        static int LASTWATCHED_MS = 10;
        static int POSTER = 11;
        static int CONTENTRATING = 12;
        static int STATUS = 13;
        static int RUNTIME = 14;
        static int NETWORK = 15;
        static int IMDBID = 16;
        static int TRAKT_ID = 17;
        static int FIRSTAIRED = 18;
        static int RATING_USER = 19;
        static int LANGUAGE = 20;
        // Full dump only
        static int OVERVIEW = 21;
        static int RATING_GLOBAL = 22;
        static int RATING_VOTES = 23;
        static int GENRES = 24;
        static int LAST_UPDATED = 25;
        static int LAST_EDITED = 26;
    }

    public static class EpisodesQuery {
        static String[] PROJECTION = new String[]{
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.TITLE,
                Episodes.FIRSTAIREDMS,
                Episodes.IMDBID,
                Episodes.DVDNUMBER,
                Episodes.RATING_USER
        };
        static String[] PROJECTION_FULL = new String[]{
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.TITLE,
                Episodes.FIRSTAIREDMS,
                Episodes.IMDBID,
                Episodes.DVDNUMBER,
                Episodes.RATING_USER,
                // Full dump only
                Episodes.OVERVIEW,
                Episodes.IMAGE,
                Episodes.WRITERS,
                Episodes.GUESTSTARS,
                Episodes.DIRECTORS,
                Episodes.RATING_GLOBAL,
                Episodes.RATING_VOTES,
                Episodes.LAST_EDITED
        };

        static String SORT = Episodes.NUMBER + " ASC";

        static int ID = 0;
        static int NUMBER = 1;
        static int NUMBER_ABSOLUTE = 2;
        static int WATCHED = 3;
        static int COLLECTED = 4;
        static int TITLE = 5;
        static int FIRSTAIRED = 6;
        static int IMDBID = 7;
        static int NUMBER_DVD = 8;
        static int RATING_USER = 9;
        // Full dump only
        static int OVERVIEW = 10;
        static int IMAGE = 11;
        static int WRITERS = 12;
        static int GUESTSTARS = 13;
        static int DIRECTORS = 14;
        static int RATING_GLOBAL = 15;
        static int RATING_VOTES = 16;
        static int LAST_EDITED = 17;
    }

    public static class ListsQuery {
        static String[] PROJECTION = new String[]{
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
        };

        static int ID = 0;
        static int NAME = 1;
        static int ORDER = 2;
    }

    public static class ListItemsQuery {
        static String[] PROJECTION = new String[]{
                ListItems.LIST_ITEM_ID, SeriesGuideContract.Lists.LIST_ID, ListItems.ITEM_REF_ID,
                ListItems.TYPE
        };

        static String SELECTION = SeriesGuideContract.Lists.LIST_ID + "=?";

        static int ID = 0;
        static int LIST_ID = 1;
        static int ITEM_REF_ID = 2;
        static int TYPE = 3;
    }

    public static class MoviesQuery {
        static String[] PROJECTION = new String[]{
                Movies._ID,
                Movies.TMDB_ID,
                Movies.IMDB_ID,
                Movies.TITLE,
                Movies.RELEASED_UTC_MS,
                Movies.RUNTIME_MIN,
                Movies.POSTER,
                Movies.IN_COLLECTION,
                Movies.IN_WATCHLIST,
                Movies.WATCHED,
                Movies.OVERVIEW
        };

        static String SORT_ORDER = Movies.TITLE + " COLLATE NOCASE ASC";

        static int TMDB_ID = 1;
        static int IMDB_ID = 2;
        static int TITLE = 3;
        static int RELEASED_UTC_MS = 4;
        static int RUNTIME_MIN = 5;
        static int POSTER = 6;
        static int IN_COLLECTION = 7;
        static int IN_WATCHLIST = 8;
        static int WATCHED = 9;
        // only in FULL dump
        static int OVERVIEW = 10;
    }
}
