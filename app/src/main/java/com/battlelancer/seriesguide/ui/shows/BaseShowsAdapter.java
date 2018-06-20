package com.battlelancer.seriesguide.ui.shows;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Base adapter for the show item layout.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    public interface OnItemClickListener {
        void onClick(View view, ShowViewHolder viewHolder);

        void onFavoriteClick(int showTvdbId, boolean isFavorite);
    }

    protected OnItemClickListener onItemClickListener;
    private final VectorDrawableCompat drawableStar;
    private final VectorDrawableCompat drawableStarZero;

    protected BaseShowsAdapter(Activity activity, OnItemClickListener listener) {
        super(activity, null, 0);
        this.onItemClickListener = listener;

        Resources.Theme theme = activity.getTheme();
        drawableStar = ViewTools.vectorIconActive(activity, theme,
                R.drawable.ic_star_black_24dp);
        drawableStarZero = ViewTools.vectorIconActive(activity, theme,
                R.drawable.ic_star_border_black_24dp);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ShowViewHolder viewHolder = new ShowViewHolder(v, onItemClickListener);
        v.setTag(viewHolder);

        return v;
    }

    protected void setFavoriteState(ImageView view, boolean isFavorite) {
        view.setImageDrawable(isFavorite ? drawableStar : drawableStarZero);
        view.setContentDescription(view.getContext()
                .getString(isFavorite ? R.string.context_unfavorite : R.string.context_favorite));
    }

    protected void setRemainingCount(TextView textView, int unwatched) {
        if (unwatched > 0) {
            textView.setText(textView.getResources()
                    .getQuantityString(R.plurals.remaining_episodes_plural, unwatched, unwatched));
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText(null);
            textView.setVisibility(View.GONE);
        }
    }

    public static class ShowViewHolder {

        private TextView name;
        private TextView timeAndNetwork;
        private TextView episode;
        private TextView episodeTime;
        private TextView remainingCount;
        private ImageView poster;
        private ImageView favorited;
        private ImageView contextMenu;

        private int showTvdbId;
        private int episodeTvdbId;
        private boolean isFavorited;
        private boolean isHidden;

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }

        public TextView getTimeAndNetwork() {
            return timeAndNetwork;
        }

        public void setTimeAndNetwork(TextView timeAndNetwork) {
            this.timeAndNetwork = timeAndNetwork;
        }

        public TextView getEpisode() {
            return episode;
        }

        public void setEpisode(TextView episode) {
            this.episode = episode;
        }

        public TextView getEpisodeTime() {
            return episodeTime;
        }

        public void setEpisodeTime(TextView episodeTime) {
            this.episodeTime = episodeTime;
        }

        public TextView getRemainingCount() {
            return remainingCount;
        }

        public void setRemainingCount(TextView remainingCount) {
            this.remainingCount = remainingCount;
        }

        public ImageView getPoster() {
            return poster;
        }

        public void setPoster(ImageView poster) {
            this.poster = poster;
        }

        public ImageView getFavorited() {
            return favorited;
        }

        public void setFavorited(ImageView favorited) {
            this.favorited = favorited;
        }

        public ImageView getContextMenu() {
            return contextMenu;
        }

        public void setContextMenu(ImageView contextMenu) {
            this.contextMenu = contextMenu;
        }

        public int getShowTvdbId() {
            return showTvdbId;
        }

        public void setShowTvdbId(int showTvdbId) {
            this.showTvdbId = showTvdbId;
        }

        public int getEpisodeTvdbId() {
            return episodeTvdbId;
        }

        public void setEpisodeTvdbId(int episodeTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
        }

        public boolean isFavorited() {
            return isFavorited;
        }

        public void setFavorited(boolean favorited) {
            isFavorited = favorited;
        }

        public boolean isHidden() {
            return isHidden;
        }

        public void setHidden(boolean hidden) {
            isHidden = hidden;
        }

        public OnItemClickListener getClickListener() {
            return clickListener;
        }

        public void setClickListener(OnItemClickListener clickListener) {
            this.clickListener = clickListener;
        }

        private OnItemClickListener clickListener;

        public ShowViewHolder(View v, OnItemClickListener onItemClickListener) {
            name = v.findViewById(R.id.seriesname);
            timeAndNetwork = v.findViewById(R.id.textViewShowsTimeAndNetwork);
            episode = v.findViewById(R.id.TextViewShowListNextEpisode);
            episodeTime = v.findViewById(R.id.episodetime);
            remainingCount = v.findViewById(R.id.textViewShowsRemaining);
            poster = v.findViewById(R.id.showposter);
            favorited = v.findViewById(R.id.favoritedLabel);
            contextMenu = v.findViewById(R.id.imageViewShowsContextMenu);
            clickListener = onItemClickListener;

            // favorite star
            favorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onFavoriteClick(showTvdbId, !isFavorited);
                    }
                }
            });
            // context menu
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onClick(v, ShowViewHolder.this);
                    }
                }
            });
        }
    }
}
