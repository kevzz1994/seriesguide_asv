package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays upcoming or recent episodes in a scrollable grid, by default grouped by day.
 */
public class CalendarFragment extends Fragment
        implements OnItemClickListener, OnSharedPreferenceChangeListener,
        AdapterView.OnItemLongClickListener {

    private static final String TAG = "Calendar";
    private static final int CONTEXT_FLAG_WATCHED_ID = 0;
    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;
    private static final int CONTEXT_CHECKIN_ID = 2;
    private static final int CONTEXT_COLLECTION_ADD_ID = 3;
    private static final int CONTEXT_COLLECTION_REMOVE_ID = 4;

    private static final int ACTIVITY_DAY_LIMIT = 30;

    private StickyGridHeadersGridView gridView;
    private CalendarAdapter adapter;
    private ImageView imageViewTapIndicator;
    private Handler handler;
    private String type;

    /**
     * Data which has to be passed when creating {@link CalendarFragment}. All Bundle extras are
     * strings, except LOADER_ID and EMPTY_STRING_ID.
     */
    public interface InitBundle {
        String TYPE = "type";
        String ANALYTICS_TAG = "analyticstag";
        String LOADER_ID = "loaderid";
        String EMPTY_STRING_ID = "emptyid";
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getArguments().getString(InitBundle.TYPE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView emptyView = v.findViewById(R.id.emptyViewCalendar);
        emptyView.setText(getString(getArguments().getInt(InitBundle.EMPTY_STRING_ID)));

        gridView = v.findViewById(R.id.gridViewCalendar);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        gridView.setEmptyView(emptyView);
        gridView.setAreHeadersSticky(false);

        VectorDrawableCompat drawableTouch = ViewTools.vectorIconInactive(getContext(),
                getActivity().getTheme(),
                R.drawable.ic_swap_vert_black_24dp);
        imageViewTapIndicator = v.findViewById(R.id.imageViewCalendarTapIndicator);
        imageViewTapIndicator.setImageDrawable(drawableTouch);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new CalendarAdapter(getActivity(), itemClickListener);

        boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
        configureCalendar(gridView, adapter, infiniteScrolling);

        // setup grid view
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // prevent stale upcoming/recent episodes, also:
        /*
          Workaround for loader issues on config changes. For some reason the
          CursorLoader holds on to a cursor with old data. See
          https://github.com/UweTrottmann/SeriesGuide/issues/257.
         */
        boolean isLoaderExists = getLoaderManager().getLoader(getLoaderId()) != null;
        getLoaderManager().initLoader(getLoaderId(), null, calendarLoaderCallbacks);
        if (isLoaderExists) {
            requery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // avoid CPU activity
        schedulePeriodicDataRefresh(false);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.calendar_menu, menu);

        VectorDrawableCompat visibilitySettingsIcon = ViewTools.vectorIconWhite(
                getActivity(), getActivity().getTheme(), R.drawable.ic_visibility_black_24dp);
        menu.findItem(R.id.menu_calendar_visibility).setIcon(visibilitySettingsIcon);

        // set menu items to current values
        Context context = getContext();
        menu.findItem(R.id.menu_action_calendar_onlyfavorites)
                .setChecked(CalendarSettings.isOnlyFavorites(context));
        menu.findItem(R.id.menu_action_calendar_onlycollected)
                .setChecked(CalendarSettings.isOnlyCollected(context));
        menu.findItem(R.id.menu_action_calendar_nospecials)
                .setChecked(DisplaySettings.isHidingSpecials(context));
        menu.findItem(R.id.menu_action_calendar_nowatched)
                .setChecked(CalendarSettings.isHidingWatchedEpisodes(context));
        menu.findItem(R.id.menu_action_calendar_infinite)
                .setChecked(CalendarSettings.isInfiniteScrolling(context));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_calendar_onlyfavorites) {
            toggleFilterSetting(item, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS);
            Utils.trackAction(getActivity(), TAG, "Only favorite shows Toggle");
            return true;
        } else if (itemId == R.id.menu_action_calendar_onlycollected) {
            toggleFilterSetting(item, CalendarSettings.KEY_ONLY_COLLECTED);
            Utils.trackAction(getActivity(), TAG, "Only calendar shows Toggle");
            return true;
        } else if (itemId == R.id.menu_action_calendar_nospecials) {
            toggleFilterSetting(item, DisplaySettings.KEY_HIDE_SPECIALS);
            Utils.trackAction(getActivity(), TAG, "Hide specials Toggle");
            return true;
        } else if (itemId == R.id.menu_action_calendar_nowatched) {
            toggleFilterSetting(item, CalendarSettings.KEY_HIDE_WATCHED_EPISODES);
            Utils.trackAction(getActivity(), TAG, "Hide watched Toggle");
            return true;
        } else if (itemId == R.id.menu_action_calendar_infinite) {
            toggleFilterSetting(item, CalendarSettings.KEY_INFINITE_SCROLLING);
            Utils.trackAction(getActivity(), TAG, "Infinite Scrolling Toggle");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleFilterSetting(MenuItem item, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(key, !item.isChecked()).apply();

        // refresh filter icon state
        getActivity().invalidateOptionsMenu();
    }

    private int getLoaderId() {
        return getArguments().getInt("loaderid");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int episodeId = (int) id;

        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        Utils.startActivityWithAnimation(getActivity(), intent, view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
            final long id) {
        if (!isResumed()) {
            // guard against being called after fragment is paged away (multi-touch)
            // adapter cursor might no longer have data
            return false;
        }

        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        Menu menu = popupMenu.getMenu();

        Cursor episode = adapter.getItem(position);
        if (episode == null) {
            return false;
        }

        // only display the action appropriate for the items current state
        if (EpisodeTools.isWatched(episode.getInt(CalendarQuery.WATCHED))) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 0, R.string.action_unwatched);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 0, R.string.action_watched);
        }
        if (EpisodeTools.isCollected(episode.getInt(CalendarQuery.COLLECTED))) {
            menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 1, R.string.action_collection_remove);
        } else {
            menu.add(0, CONTEXT_COLLECTION_ADD_ID, 1, R.string.action_collection_add);
        }
        // display check-in if only trakt is connected
        if (TraktCredentials.get(view.getContext()).hasCredentials()
                && !HexagonSettings.isEnabled(view.getContext())) {
            menu.add(0, CONTEXT_CHECKIN_ID, 2, R.string.checkin);
        }

        final int showTvdbId = episode.getInt(CalendarQuery.SHOW_ID);
        final int episodeTvdbId = episode.getInt(CalendarQuery._ID);
        final int seasonNumber = episode.getInt(CalendarQuery.SEASON);
        final int episodeNumber = episode.getInt(CalendarQuery.NUMBER);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case CONTEXT_CHECKIN_ID: {
                        checkInEpisode((int) id);
                        return true;
                    }
                    case CONTEXT_FLAG_WATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_FLAG_UNWATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                    case CONTEXT_COLLECTION_ADD_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_COLLECTION_REMOVE_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                }
                return false;
            }
        });

        popupMenu.show();

        return true;
    }

    private void checkInEpisode(int episodeTvdbId) {
        CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(), episodeTvdbId);
        if (f != null && isResumed()) {
            f.show(getFragmentManager(), "checkin-dialog");
        }
    }

    private void updateEpisodeCollectionState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean addToCollection) {
        EpisodeTools.episodeCollected(getContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episodeNumber, addToCollection);
    }

    private void updateEpisodeWatchedState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean isWatched) {
        EpisodeTools.episodeWatched(getContext(), showTvdbId, episodeTvdbId,
                seasonNumber, episodeNumber,
                isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if ((CalendarType.UPCOMING.equals(type)
                && event.position == ShowsActivity.InitBundle.INDEX_TAB_UPCOMING) ||
                CalendarType.RECENT.equals(type)
                        && event.position == ShowsActivity.InitBundle.INDEX_TAB_RECENT) {
            gridView.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
            configureCalendar(gridView, adapter, infiniteScrolling);
            // re-set the adapter to properly force a re-layout
            // FIXME: this should not be required, works on emulator, but not device
            // (likely race condition with requestLayout(), though that makes no sense...)
            gridView.setAdapter(adapter);
        }
        if (CalendarSettings.KEY_ONLY_FAVORITE_SHOWS.equals(key)
                || CalendarSettings.KEY_ONLY_COLLECTED.equals(key)
                || DisplaySettings.KEY_HIDE_SPECIALS.equals(key)
                || CalendarSettings.KEY_HIDE_WATCHED_EPISODES.equals(key)
                || CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            requery();
        }
    }

    private void configureCalendar(GridView gridView, CalendarAdapter adapter,
            boolean infiniteScrolling) {
        adapter.setIsShowingHeaders(!infiniteScrolling);

        gridView.setFastScrollEnabled(infiniteScrolling);
        gridView.setFastScrollAlwaysVisible(infiniteScrolling);
        Resources res = getResources();
        int paddingLeft = 0;
        int paddingRight = infiniteScrolling
                ? res.getDimensionPixelSize(R.dimen.grid_fast_scroll_padding)
                : paddingLeft;
        int paddingTopBottom = res.getDimensionPixelSize(R.dimen.default_padding);
        gridView.setPadding(paddingLeft, paddingTopBottom, paddingRight, paddingTopBottom);

        updateTapIndicatorVisibility();
    }

    private void updateTapIndicatorVisibility() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            // on L and below indicator is centered in touchable space, so do not show hint icon
            imageViewTapIndicator.setVisibility(View.GONE);
        } else {
            // show hint icon in touchable fast scroll indicator space
            boolean hasData = adapter.getCount() != 0;
            boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
            imageViewTapIndicator.setVisibility(infiniteScrolling && hasData
                    ? View.VISIBLE
                    : View.GONE);
        }
    }

    private void requery() {
        getLoaderManager().restartLoader(getLoaderId(), null, calendarLoaderCallbacks);
    }

    private void schedulePeriodicDataRefresh(boolean enableRefresh) {
        if (handler == null) {
            handler = new Handler();
        }
        handler.removeCallbacks(dataRefreshRunnable);
        if (enableRefresh) {
            handler.postDelayed(dataRefreshRunnable, 5 * DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private Runnable dataRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                getLoaderManager().restartLoader(getLoaderId(), null, calendarLoaderCallbacks);
            }
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> calendarLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            boolean isOnlyCollected = CalendarSettings.isOnlyCollected(getActivity());
            boolean isOnlyFavorites = CalendarSettings.isOnlyFavorites(getActivity());
            boolean isOnlyUnwatched = CalendarSettings.isHidingWatchedEpisodes(getActivity());
            boolean isInfiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());

            // infinite or 30 days activity stream
            String[][] queryArgs = buildActivityQuery(getActivity(), type, isOnlyCollected,
                    isOnlyFavorites, isOnlyUnwatched, isInfiniteScrolling);

            // prevent upcoming/recent episodes from becoming stale
            schedulePeriodicDataRefresh(true);

            return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                    CalendarQuery.PROJECTION, queryArgs[0][0], queryArgs[1],
                    queryArgs[2][0]);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
            updateTapIndicatorVisibility();
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    /**
     * Returns an array of size 3. The built query is stored in {@code [0][0]}, the built selection
     * args in {@code [1]} and the sort order in {@code [2][0]}.
     *
     * @param type A {@link CalendarType}, defaults to UPCOMING.
     * @param isInfinite If false, limits the release time range of returned episodes to {@link
     * #ACTIVITY_DAY_LIMIT} days from today.
     */
    public static String[][] buildActivityQuery(Context context, String type,
            boolean isOnlyCollected, boolean isOnlyFavorites, boolean isOnlyUnwatched,
            boolean isInfinite) {
        // go an hour back in time, so episodes move to recent one hour late
        long recentThreshold = TimeTools.getCurrentTime(context) - DateUtils.HOUR_IN_MILLIS;

        StringBuilder query;
        String[] selectionArgs;
        String sortOrder;
        long timeThreshold;

        if (CalendarType.RECENT.equals(type)) {
            query = new StringBuilder(CalendarQuery.QUERY_RECENT);
            sortOrder = CalendarQuery.SORTING_RECENT;
            if (isInfinite) {
                // to the past!
                timeThreshold = Long.MIN_VALUE;
            } else {
                // last x days
                timeThreshold = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS
                        * ACTIVITY_DAY_LIMIT;
            }
        } else {
            query = new StringBuilder(CalendarQuery.QUERY_UPCOMING);
            sortOrder = CalendarQuery.SORTING_UPCOMING;
            if (isInfinite) {
                // to the future!
                timeThreshold = Long.MAX_VALUE;
            } else {
                // coming x days
                timeThreshold = System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS
                        * ACTIVITY_DAY_LIMIT;
            }
        }

        selectionArgs = new String[] {
                String.valueOf(recentThreshold), String.valueOf(timeThreshold)
        };

        // append only favorites selection if necessary
        if (isOnlyFavorites) {
            query.append(" AND ").append(SeriesGuideContract.Shows.SELECTION_FAVORITES);
        }

        // append no specials selection if necessary
        boolean isNoSpecials = DisplaySettings.isHidingSpecials(context);
        if (isNoSpecials) {
            query.append(" AND ").append(Episodes.SELECTION_NO_SPECIALS);
        }

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query.append(" AND ").append(Episodes.SELECTION_UNWATCHED);
        }

        // only show collected episodes
        if (isOnlyCollected) {
            query.append(" AND ").append(Episodes.SELECTION_COLLECTED);
        }

        // build result array
        String[][] results = new String[3][];
        results[0] = new String[] {
                query.toString()
        };
        results[1] = selectionArgs;
        results[2] = new String[] {
                sortOrder
        };
        return results;
    }

    private CalendarAdapter.ItemClickListener itemClickListener
            = new CalendarAdapter.ItemClickListener() {
        @Override
        public void onWatchedBoxClick(int episodePosition, boolean isWatched) {
            Cursor episode = adapter.getItem(episodePosition);
            if (episode == null) {
                return;
            }

            int showTvdbId = episode.getInt(CalendarQuery.SHOW_ID);
            int episodeTvdbId = episode.getInt(CalendarQuery._ID);
            int seasonNumber = episode.getInt(CalendarQuery.SEASON);
            int episodeNumber = episode.getInt(CalendarQuery.NUMBER);

            updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber, episodeNumber,
                    !isWatched);
        }
    };
}
