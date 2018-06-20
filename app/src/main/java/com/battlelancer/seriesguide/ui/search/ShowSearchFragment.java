package com.battlelancer.seriesguide.ui.search;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.PopupMenu;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter;
import com.battlelancer.seriesguide.ui.shows.ShowMenuItemClickListener;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.TimeTools;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays show search results.
 */
public class ShowSearchFragment extends BaseSearchFragment {

    private ShowResultsAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ShowResultsAdapter(getActivity(), onItemClickListener);
        gridView.setAdapter(adapter);

        // load for given query or restore last loader (ignoring args)
        getLoaderManager().initLoader(SearchActivity.SHOWS_LOADER_ID, loaderArgs,
                searchLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = OverviewActivity.intentShow(getContext(), (int) id);
        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.getWidth(),
                        view.getHeight()).toBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SearchActivity.SearchQueryEvent event) {
        getLoaderManager().restartLoader(SearchActivity.SHOWS_LOADER_ID, event.args,
                searchLoaderCallbacks);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == SearchActivity.TAB_POSITION_SHOWS) {
            gridView.smoothScrollToPosition(0);
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor>
            searchLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            loaderArgs = args;
            String query = args.getString(SearchManager.QUERY);
            if (TextUtils.isEmpty(query)) {
                // empty query selects shows with next episodes before this point in time
                String customTimeInOneHour = String.valueOf(TimeTools.getCurrentTime(getActivity())
                        + DateUtils.HOUR_IN_MILLIS);
                return new CursorLoader(getActivity(), SeriesGuideContract.Shows.CONTENT_URI,
                        ShowResultsAdapter.PROJECTION,
                        SeriesGuideContract.Shows.NEXTEPISODE + "!='' AND "
                                + SeriesGuideContract.Shows.HIDDEN + "=0 AND "
                                + SeriesGuideContract.Shows.NEXTAIRDATEMS + "<?",
                        new String[]{customTimeInOneHour},
                        SeriesGuideContract.Shows.SORT_LATEST_EPISODE);
            } else {
                Uri uri = SeriesGuideContract.Shows.CONTENT_URI_FILTER.buildUpon()
                        .appendPath(query)
                        .build();
                return new CursorLoader(getActivity(), uri,
                        ShowResultsAdapter.PROJECTION, null, null, null);
            }
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private BaseShowsAdapter.OnItemClickListener onItemClickListener
            = new BaseShowsAdapter.OnItemClickListener() {
        @Override
        public void onClick(View view, BaseShowsAdapter.ShowViewHolder viewHolder) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.shows_popup_menu);

            // show/hide some menu items depending on show properties
            Menu menu = popupMenu.getMenu();
            menu.findItem(R.id.menu_action_shows_favorites_add)
                    .setVisible(!viewHolder.isFavorited());
            menu.findItem(R.id.menu_action_shows_favorites_remove)
                    .setVisible(viewHolder.isFavorited());
            menu.findItem(R.id.menu_action_shows_hide).setVisible(!viewHolder.isHidden());
            menu.findItem(R.id.menu_action_shows_unhide).setVisible(viewHolder.isHidden());

            // hide unused actions
            menu.findItem(R.id.menu_action_shows_watched_next).setVisible(false);

            popupMenu.setOnMenuItemClickListener(
                    new ShowMenuItemClickListener(getContext(),
                            getFragmentManager(), viewHolder.getShowTvdbId(), viewHolder.getEpisodeTvdbId(),
                            ListsActivity.TAG));
            popupMenu.show();
        }

        @Override
        public void onFavoriteClick(int showTvdbId, boolean isFavorite) {
            SgApp.getServicesComponent(getContext()).showTools()
                    .storeIsFavorite(showTvdbId, isFavorite);
        }
    };
}
