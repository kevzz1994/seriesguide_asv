package com.battlelancer.seriesguide.ui.movies;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MoviesDiscoverFragment extends Fragment {

    @BindView(R.id.swipeRefreshLayoutMoviesDiscover) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerViewMoviesDiscover) RecyclerView recyclerView;

    private MoviesDiscoverAdapter adapter;
    private GridLayoutManager layoutManager;
    private Unbinder unbinder;

    public MoviesDiscoverFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies_discover, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setRefreshing(false);
        ViewTools.setSwipeRefreshLayoutColors(getActivity().getTheme(), swipeRefreshLayout);

        adapter = new MoviesDiscoverAdapter(getContext(),
                new MovieItemClickListener(getActivity()));

        layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 2, 6);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_LINK) {
                    return 3;
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_HEADER) {
                    return layoutManager.getSpanCount();
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_MOVIE) {
                    return 2;
                }
                return 0;
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, nowPlayingLoaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // when switching tabs while still showing refresh animation, old content remains stuck
        // so force clear the drawing cache and animation: http://stackoverflow.com/a/27073879
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.movies_discover_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_movies_search_change_language) {
            MovieLocalizationDialogFragment.show(getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLanguageChanged(
            MovieLocalizationDialogFragment.LocalizationChangedEvent event) {
        getLoaderManager().restartLoader(0, null, nowPlayingLoaderCallbacks);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(MoviesActivity.MoviesTabClickEvent event) {
        if (event.position == MoviesActivity.TAB_POSITION_DISCOVER) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    private static class MovieItemClickListener extends MoviesSearchFragment.MovieItemClickListener
            implements MoviesDiscoverAdapter.ItemClickListener {

        MovieItemClickListener(Activity activity) {
            super(activity);
        }

        @Override
        public void onClickLink(MoviesDiscoverLink link, View anchor) {
            Intent intent = new Intent(getActivity(), MoviesSearchActivity.class);
            intent.putExtra(MoviesSearchActivity.EXTRA_ID_LINK, link.id);
            Utils.startActivityWithAnimation(getActivity(), intent, anchor);
        }
    }

    private LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result> nowPlayingLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TmdbMoviesLoader(getContext(),
                    MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT, null);
        }

        @Override
        public void onLoadFinished(Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            adapter.updateMovies(data.results);
        }

        @Override
        public void onLoaderReset(Loader<TmdbMoviesLoader.Result> loader) {
            adapter.updateMovies(null);
        }
    };

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener
            = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getLoaderManager().restartLoader(0, null, nowPlayingLoaderCallbacks);
        }
    };
}
