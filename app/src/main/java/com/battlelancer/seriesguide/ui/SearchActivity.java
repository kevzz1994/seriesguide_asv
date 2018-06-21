package com.battlelancer.seriesguide.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.ui.episodes.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.search.EpisodeSearchFragment;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.ui.search.ShowSearchFragment;
import com.battlelancer.seriesguide.ui.search.ShowsDiscoverFragment;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import com.google.android.gms.actions.SearchIntents;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Handles search intents and displays a {@link EpisodeSearchFragment} when needed or redirects
 * directly to an {@link EpisodeDetailsActivity}.
 */
public class SearchActivity extends BaseNavDrawerActivity implements
        AddShowDialogFragment.OnAddShowListener {

    /**
     * Which tab to select upon launch.
     */
    public static final String EXTRA_DEFAULT_TAB = "default_tab";

    public static final int TAB_POSITION_SHOWS = 0;
    public static final int TAB_POSITION_EPISODES = 1;
    public static final int TAB_POSITION_SEARCH = 2;

    public static final int SHOWS_LOADER_ID = 100;
    public static final int EPISODES_LOADER_ID = 101;
    public static final int TRAKT_BASE_LOADER_ID = 200;

    @BindView(R.id.containerSearchBar) View searchContainer;
    @BindView(R.id.editTextSearchBar) AutoCompleteTextView searchView;
    @BindView(R.id.imageButtonSearchClear) View clearButton;
    @BindView(R.id.tabsSearch) SlidingTabLayout tabs;
    @BindView(R.id.pagerSearch) ViewPager viewPager;

    public AutoCompleteTextView getSearchView() {
        return searchView;
    }

    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;
    private boolean tvdbSearchVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setupActionBar();
        setupNavDrawer();

        setupViews(savedInstanceState == null);

        handleBeamIntent(getIntent());
        handleSearchIntent(getIntent());
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void setupViews(boolean mayShowKeyboard) {
        ButterKnife.bind(this);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText(null);
                searchView.requestFocus();
            }
        });

        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    triggerTvdbSearch();
                    return true;
                }
                return false;
            }
        });

        // manually retrieve the auto complete view popup background to override the theme
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.autoCompleteTextViewStyle, outValue, true);
        int[] attributes = new int[]{android.R.attr.popupBackground};
        TypedArray a = getTheme().obtainStyledAttributes(outValue.data, attributes);
        if (a.hasValue(0)) {
            searchView.setDropDownBackgroundDrawable(a.getDrawable(0));
        }
        a.recycle();

        // setup search history (only used by TVDb search)
        searchHistory = new SearchHistory(this, SearchSettings.KEY_SUFFIX_THETVDB);
        searchHistoryAdapter = new ArrayAdapter<>(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light
                        ? R.layout.item_dropdown_light : R.layout.item_dropdown,
                searchHistory.getSearchHistory());
        searchView.setThreshold(1);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                triggerTvdbSearch();
            }
        });
        // set in code as XML is overridden
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchView.dismissDropDown();

        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                viewPager, tabs);
        tabs.setOnPageChangeListener(pageChangeListener);
        tabs.setOnTabClickListener(new SlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (viewPager.getCurrentItem() == position) {
                    EventBus.getDefault().post(new TabClickEvent(position));
                }
            }
        });

        tabsAdapter.addTab(R.string.shows, ShowSearchFragment.class, null);
        tabsAdapter.addTab(R.string.episodes, EpisodeSearchFragment.class, null);
        tabsAdapter.addTab(R.string.title_discover, ShowsDiscoverFragment.class, null);
        tabsAdapter.notifyTabsChanged();

        // set default tab
        if (getIntent() != null && getIntent().getExtras() != null) {
            int defaultTab = getIntent().getExtras().getInt(EXTRA_DEFAULT_TAB);
            if (defaultTab < tabsAdapter.getCount()) {
                viewPager.setCurrentItem(defaultTab);
            }
            if (mayShowKeyboard &&
                    (defaultTab == TAB_POSITION_SHOWS || defaultTab == TAB_POSITION_EPISODES)) {
                ViewTools.showSoftKeyboardOnSearchView(this, searchView);
            }
        } else if (mayShowKeyboard) {
            // also show keyboard when showing first tab (added tab)
            ViewTools.showSoftKeyboardOnSearchView(this, searchView);
        }
    }

    private final ViewPager.OnPageChangeListener pageChangeListener
            = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            // only display search box if it can be used
            boolean searchVisible = position <= TAB_POSITION_SEARCH;
            searchContainer.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
            if (searchVisible) {
                tvdbSearchVisible = position == TAB_POSITION_SEARCH;
                searchView.setAdapter(tvdbSearchVisible ? searchHistoryAdapter : null);
                searchView.setHint(
                        tvdbSearchVisible ? R.string.checkin_searchhint : R.string.search);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleBeamIntent(intent);
        handleSearchIntent(intent);
    }

    /**
     * Handles Android Beam intents, extracts the beamed show from the NDEF Message and displays an
     * add dialog for the show.
     */
    private void handleBeamIntent(@Nullable Intent intent) {
        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return;
        }

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length == 0) {
            Utils.trackCustomEvent(this, "Beam", "Failed", "Data null or zero length");
            return; // corrupted or invalid data
        }

        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        int showTvdbId;
        try {
            showTvdbId = Integer.valueOf(new String(msg.getRecords()[0].getPayload()));
        } catch (NumberFormatException e) {
            Utils.trackCustomEvent(this, "Beam", "Failed",
                    "NumberFormatException: " + e.getMessage());
            return;
        }

        // display add dialog
        AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
        Utils.trackCustomEvent(this, "Beam", "Success", null);
    }

    private void handleSearchIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        // global or Google Now voice search
        if (Intent.ACTION_SEARCH.equals(action) || SearchIntents.ACTION_SEARCH.equals(action)) {
            Intent launchIntent = getIntent();

            // searching episodes within a show?
            Bundle appData = launchIntent.getBundleExtra(SearchManager.APP_DATA);
            if (appData != null) {
                String showTitle = appData.getString(EpisodeSearchFragment.InitBundle.SHOW_TITLE);
                if (!TextUtils.isEmpty(showTitle)) {
                    // change title + switch to episodes tab if show restriction was submitted
                    viewPager.setCurrentItem(TAB_POSITION_EPISODES);
                }
            }

            String query = launchIntent.getStringExtra(SearchManager.QUERY);
            searchView.setText(query);
            triggerLocalSearch(query);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data == null) {
                // no data, just stay inside search activity
                return;
            }
            String id = data.getLastPathSegment();
            displayEpisode(id);
            finish();
        } else if (Intent.ACTION_SEND.equals(action)) {
            // text share intents from other apps
            if ("text/plain".equals(intent.getType())) {
                handleSharedText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }
    }

    private void handleSharedText(@Nullable String sharedText) {
        if (TextUtils.isEmpty(sharedText)) {
            return;
        }

        // try to match TVDB URLs
        // match season and episode pages first
        Pattern tvdbSeriesIdPattern = Pattern.compile("thetvdb\\.com.*?seriesid=([0-9]*)");
        int showTvdbId = matchShowTvdbId(tvdbSeriesIdPattern, sharedText);
        if (showTvdbId <= 0) {
            // match show pages
            Pattern tvdbIdPattern = Pattern.compile("thetvdb\\.com.*?id=([0-9]*)");
            showTvdbId = matchShowTvdbId(tvdbIdPattern, sharedText);
        }
        if (showTvdbId <= 0) {
            // try to match trakt search URLs (the ones shared by this app)
            Pattern traktShowIdPattern =
                    Pattern.compile("trakt\\.tv\\/search\\/tvdb\\/([0-9]*)\\?id_type=show");
            showTvdbId = matchShowTvdbId(traktShowIdPattern, sharedText);
        }

        if (showTvdbId > 0) {
            // found an id, display the add dialog
            AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
        } else {
            // no id, populate the search field instead
            viewPager.setCurrentItem(TAB_POSITION_SEARCH);
            searchView.setText(sharedText);
            triggerTvdbSearch();
            triggerLocalSearch(sharedText);
        }
    }

    private int matchShowTvdbId(Pattern pattern, String text) {
        int showTvdbId = -1;

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                showTvdbId = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return showTvdbId;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // set after view states are restored to avoid triggering
        searchView.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onPause() {
        super.onPause();
        searchView.removeTextChangedListener(textWatcher);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            triggerLocalSearch(s);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private void triggerLocalSearch(@Nullable CharSequence queryRaw) {
        String query = TextUtils.isEmpty(queryRaw) ? "" : queryRaw.toString();

        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, query);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                args.putBundle(SearchManager.APP_DATA, appData);
            }
        }

        EventBus.getDefault().postSticky(new SearchQueryEvent(args));
    }

    public void triggerTvdbSearch() {
        if (tvdbSearchVisible) {
            searchView.dismissDropDown();
            // extract and post query
            String query = searchView.getText().toString().trim();
            EventBus.getDefault().postSticky(new SearchQuerySubmitEvent(query));
            // update history
            if (query.length() > 0) {
                if (searchHistory.saveRecentSearch(query)) {
                    searchHistoryAdapter.clear();
                    searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
                }
            }
        }
    }

    private void displayEpisode(String episodeTvdbId) {
        Intent i = new Intent(this, EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, Integer.valueOf(episodeTvdbId));
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove any stored initial queries so they are not used when re-creating
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent.class);
        EventBus.getDefault().removeStickyEvent(SearchQuerySubmitEvent.class);
    }

    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance().performAddTask(this, show);
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ClearSearchHistoryEvent event) {
        if (searchHistory != null && searchHistoryAdapter != null) {
            searchHistory.clearHistory();
            searchHistoryAdapter.clear();
            // setting text to null seems to fix the dropdown from not clearing
            searchView.setText(null);
        }
    }

    @Override
    protected View getSnackbarParentView() {
        if (AndroidUtils.isLollipopOrHigher()) {
            return findViewById(R.id.coordinatorLayoutSearch);
        } else {
            return super.getSnackbarParentView();
        }
    }

    /** Used by {@link ShowsDiscoverFragment} to indicate the search history should be cleared. */
    public static class ClearSearchHistoryEvent {
    }

    /**
     * Used by {@link ShowSearchFragment} and {@link EpisodeSearchFragment} to search as the user
     * types.
     */
    public static class SearchQueryEvent {
        public final Bundle args;

        public SearchQueryEvent(Bundle args) {
            this.args = args;
        }
    }

    /**
     * Used by {@link ShowsDiscoverFragment} to submit a query. Unlike local search it is not type
     * and search.
     */
    public static class SearchQuerySubmitEvent {
        public final String query;

        public SearchQuerySubmitEvent(String query) {
            this.query = query;
        }
    }
}
