package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import java.text.DateFormat;
import java.util.Date;

class MoviesCursorAdapter extends CursorAdapter {

    private final int uniqueId;
    private final String tmdbImageBaseUrl;

    private final DateFormat dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat();

    private final PopupMenuClickListener popupMenuClickListener;

    interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int movieTmdbId);
    }

    MoviesCursorAdapter(Context context, PopupMenuClickListener popupMenuClickListener,
            int uniqueId) {
        super(context, null, 0);
        this.uniqueId = uniqueId;
        this.popupMenuClickListener = popupMenuClickListener;

        // figure out which size of posters to load based on screen density
        if (DisplaySettings.isVeryHighDensityScreen(context)) {
            tmdbImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342;
        } else {
            tmdbImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W154;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // do not use parent layout params to avoid padding issues
        @SuppressLint("InflateParams") View v =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, null);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.title = v.findViewById(R.id.textViewMovieTitle);
        viewHolder.releaseDate = v.findViewById(R.id.textViewMovieDate);
        viewHolder.poster = v.findViewById(R.id.imageViewMoviePoster);
        viewHolder.contextMenu = v.findViewById(R.id.imageViewMovieItemContextMenu);

        v.setTag(viewHolder);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        // title
        holder.title.setText(cursor.getString(MoviesQuery.TITLE));

        // release date
        long released = cursor.getLong(MoviesQuery.RELEASED_UTC_MS);
        if (released != Long.MAX_VALUE) {
            holder.releaseDate.setText(dateFormatMovieReleaseDate.format(new Date(released)));
        } else {
            holder.releaseDate.setText("");
        }

        // load poster, cache on external storage
        String posterPath = cursor.getString(MoviesQuery.POSTER);
        // use fixed size so bitmaps can be re-used on config change
        ServiceUtils.loadWithPicasso(context, TextUtils.isEmpty(posterPath)
                ? null : tmdbImageBaseUrl + posterPath)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(holder.poster);

        // context menu
        final int movieTmdbId = cursor.getInt(MoviesQuery.TMDB_ID);
        holder.contextMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuClickListener != null) {
                    popupMenuClickListener.onPopupMenuClick(v, movieTmdbId);
                }
            }
        });

        // set unique transition names
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.poster.setTransitionName(
                    "moviesCursorAdapterPoster_" + uniqueId + "_" + movieTmdbId);
        }
    }

    public static class ViewHolder {
        private TextView title;
        private TextView releaseDate;
        private ImageView poster;
        private View contextMenu;

        public TextView getTitle() {
            return title;
        }

        public void setTitle(TextView title) {
            this.title = title;
        }

        public TextView getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(TextView releaseDate) {
            this.releaseDate = releaseDate;
        }

        public ImageView getPoster() {
            return poster;
        }

        public void setPoster(ImageView poster) {
            this.poster = poster;
        }

        public View getContextMenu() {
            return contextMenu;
        }

        public void setContextMenu(View contextMenu) {
            this.contextMenu = contextMenu;
        }
    }

    static class MoviesQuery {

        static String[] PROJECTION = { Movies._ID, Movies.TMDB_ID, Movies.TITLE, Movies.POSTER,
                Movies.RELEASED_UTC_MS };

        int ID = 0;
        static int TMDB_ID = 1;
        static int TITLE = 2;
        static int POSTER = 3;
        static int RELEASED_UTC_MS = 4;
    }
}
