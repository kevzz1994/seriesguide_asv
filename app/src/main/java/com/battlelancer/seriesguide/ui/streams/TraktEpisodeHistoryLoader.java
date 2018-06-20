package com.battlelancer.seriesguide.ui.streams;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.ui.shows.TraktRecentEpisodeHistoryLoader;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads the last few episodes watched on trakt.
 */
class TraktEpisodeHistoryLoader extends GenericSimpleLoader<TraktEpisodeHistoryLoader.Result> {

    static class Result {
        private List<HistoryEntry> results;
        private String emptyText;

        public Result(List<HistoryEntry> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }

        public List<HistoryEntry> getResults() {
            return results;
        }

        public void setResults(List<HistoryEntry> results) {
            this.results = results;
        }

        public String getEmptyText() {
            return emptyText;
        }

        public void setEmptyText(String emptyText) {
            this.emptyText = emptyText;
        }
    }

    TraktEpisodeHistoryLoader(Activity activity) {
        super(activity);
    }

    @Override
    public Result loadInBackground() {
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history = null;
        try {
            Response<List<HistoryEntry>> response = buildCall().execute();
            if (response.isSuccessful()) {
                history = response.body();
            } else {
                if (SgTrakt.isUnauthorized(getContext(), response)) {
                    return buildResultFailure(R.string.trakt_error_credentials);
                }
                SgTrakt.trackFailedRequest(getContext(), getAction(), response);
            }
        } catch (Exception e) {
            SgTrakt.trackFailedRequest(getContext(), getAction(), e);
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildResultFailure() : buildResultFailure(R.string.offline);
        }

        if (history == null) {
            return buildResultFailure();
        } else {
            return new Result(history, getContext().getString(getEmptyText()));
        }
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    @StringRes
    protected int getEmptyText() {
        return R.string.user_stream_empty;
    }

    protected Call<List<HistoryEntry>> buildCall() {
        return TraktRecentEpisodeHistoryLoader.buildUserEpisodeHistoryCall(getContext());
    }

    private Result buildResultFailure() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int emptyTextResId) {
        return new Result(null, getContext().getString(emptyTextResId));
    }
}
