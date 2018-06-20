package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.TextTools;

/**
 * Allows to check into an episode on trakt, into a show on GetGlue. Launching activities should
 * subscribe to {@link TraktTask.TraktActionCompleteEvent} to
 * display status toasts.
 */
public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    /**
     * Builds a new {@link CheckInDialogFragment} setting all values based on the given episode TVDb
     * id. Might return null.
     */
    public static CheckInDialogFragment newInstance(Context context, int episodeTvdbId) {
        CheckInDialogFragment f = null;

        final Cursor episode = context.getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                CheckInQuery.PROJECTION, null, null, null);
        if (episode != null) {
            if (episode.moveToFirst()) {
                f = new CheckInDialogFragment();

                Bundle args = new Bundle();
                args.putInt(InitBundle.EPISODE_TVDB_ID, episodeTvdbId);
                String episodeTitleWithNumbers = episode.getString(CheckInQuery.SHOW_TITLE)
                        + " "
                        + TextTools.getNextEpisodeString(context,
                        episode.getInt(CheckInQuery.SEASON),
                        episode.getInt(CheckInQuery.NUMBER),
                        episode.getString(CheckInQuery.EPISODE_TITLE));
                args.putString(InitBundle.ITEM_TITLE, episodeTitleWithNumbers);

                f.setArguments(args);
            }
            episode.close();
        }

        return f;
    }

    private static class CheckInQuery {

        static String[] PROJECTION = new String[] {
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.TITLE,
                Shows.TITLE
        };

        static int SEASON = 0;
        static int NUMBER = 1;
        static int EPISODE_TITLE = 2;
        static int SHOW_TITLE = 3;
    }

    @Override
    protected void checkInTrakt(String message) {
        new TraktTask(getContext()).checkInEpisode(
                getArguments().getInt(InitBundle.EPISODE_TVDB_ID),
                getArguments().getString(InitBundle.ITEM_TITLE),
                message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
