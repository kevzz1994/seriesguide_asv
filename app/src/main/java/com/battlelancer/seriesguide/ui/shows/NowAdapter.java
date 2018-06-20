package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.ViewTools;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sectioned adapter displaying recently watched episodes and episodes recently watched by trakt
 * friends.
 */
public class NowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected static final String TRAKT_ACTION_WATCH = "watch";

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    protected static class HistoryViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.textViewHistoryShow) public TextView show;
        @BindView(R.id.textViewHistoryEpisode) public TextView episode;
        @BindView(R.id.imageViewHistoryPoster) public ImageView poster;
        @BindView(R.id.textViewHistoryInfo) public TextView info;
        @BindView(R.id.imageViewHistoryAvatar) public ImageView avatar;
        @BindView(R.id.imageViewHistoryType) public ImageView type;

        public HistoryViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

    static class MoreViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        public TextView getTitle() {
            return title;
        }

        public void setTitle(TextView title) {
            this.title = title;
        }

        public MoreViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewNowMoreText);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        public TextView getTitle() {
            return title;
        }

        public void setTitle(TextView title) {
            this.title = title;
        }

        public HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewGridHeader);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ItemType.HISTORY, ItemType.FRIEND, ItemType.MORE_LINK, ItemType.HEADER})
    public @interface ItemType {
        int HISTORY = 1;
        int FRIEND = 2;
        int MORE_LINK = 3;
        int HEADER = 4;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ViewType.HISTORY, ViewType.MORE_LINK, ViewType.HEADER})
    public @interface ViewType {
        int HISTORY = 1;
        int MORE_LINK = 2;
        int HEADER = 3;
    }

    private final Context context;
    private final ItemClickListener listener;
    private final VectorDrawableCompat drawableWatched;
    private final VectorDrawableCompat drawableCheckin;

    private List<NowItem> dataset;
    private List<NowItem> recentlyWatched;
    private List<NowItem> friendsRecently;

    public static class NowItem {
        private Integer episodeTvdbId;
        private Integer showTvdbId;
        private Integer movieTmdbId;
        private long timestamp;
        private String title;
        private String description;
        private String network;
        private String tvdbPosterUrl;
        private String username;
        private String avatar;
        private String action;
        @ItemType public int type;

        public NowItem recentlyWatchedLocal() {
            this.type = ItemType.HISTORY;
            return this;
        }

        public NowItem recentlyWatchedTrakt(@Nullable String action) {
            this.action = action;
            this.type = ItemType.HISTORY;
            return this;
        }

        public NowItem friend(String username, String avatar, String action) {
            this.username = username;
            this.avatar = avatar;
            this.action = action;
            this.type = ItemType.FRIEND;
            return this;
        }

        public NowItem tvdbIds(Integer episodeTvdbId, Integer showTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
            this.showTvdbId = showTvdbId;
            return this;
        }

        public NowItem tmdbId(Integer movieTmdbId) {
            this.movieTmdbId = movieTmdbId;
            return this;
        }

        public NowItem displayData(long timestamp, String title, String description,
                String tvdbPosterUrl) {
            this.timestamp = timestamp;
            this.title = title;
            this.description = description;
            this.tvdbPosterUrl = tvdbPosterUrl;
            return this;
        }

        public NowItem moreLink(String title) {
            this.type = ItemType.MORE_LINK;
            this.title = title;
            return this;
        }

        public NowItem header(String title) {
            this.type = ItemType.HEADER;
            this.title = title;
            return this;
        }

        public Integer getEpisodeTvdbId() {
            return episodeTvdbId;
        }

        public void setEpisodeTvdbId(Integer episodeTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
        }

        public Integer getShowTvdbId() {
            return showTvdbId;
        }

        public void setShowTvdbId(Integer showTvdbId) {
            this.showTvdbId = showTvdbId;
        }

        public Integer getMovieTmdbId() {
            return movieTmdbId;
        }

        public void setMovieTmdbId(Integer movieTmdbId) {
            this.movieTmdbId = movieTmdbId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }

        public String getTvdbPosterUrl() {
            return tvdbPosterUrl;
        }

        public void setTvdbPosterUrl(String tvdbPosterUrl) {
            this.tvdbPosterUrl = tvdbPosterUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    public NowAdapter(Context context, ItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.dataset = new ArrayList<>();
        this.drawableWatched = ViewTools.vectorIconInactive(getContext(),
                getContext().getTheme(),
                R.drawable.ic_watch_black_16dp);
        this.drawableCheckin = ViewTools.vectorIconInactive(getContext(),
                getContext().getTheme(),
                R.drawable.ic_checkin_black_16dp);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == ViewType.HEADER) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_grid_header, viewGroup, false);
            return new HeaderViewHolder(v);
        } else if (viewType == ViewType.MORE_LINK) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_more, viewGroup, false);
            return new MoreViewHolder(v, listener);
        } else if (viewType == ViewType.HISTORY) {
            return getHistoryViewHolder(viewGroup, listener);
        } else {
            throw new IllegalArgumentException("Using unrecognized view type.");
        }
    }

    @NonNull
    protected RecyclerView.ViewHolder getHistoryViewHolder(ViewGroup viewGroup,
            ItemClickListener itemClickListener) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_history, viewGroup, false);
        return new HistoryViewHolder(v, itemClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        NowItem item = getItem(position);

        if (viewHolder instanceof HeaderViewHolder) {
            HeaderViewHolder holder = (HeaderViewHolder) viewHolder;

            holder.title.setText(item.title);
        } else if (viewHolder instanceof MoreViewHolder) {
            MoreViewHolder holder = (MoreViewHolder) viewHolder;

            holder.title.setText(item.title);
        } else if (viewHolder instanceof HistoryViewHolder) {
            HistoryViewHolder holder = (HistoryViewHolder) viewHolder;

            String time = TimeTools.formatToLocalRelativeTime(getContext(),
                    new Date(item.timestamp));
            if (item.type == ItemType.HISTORY) {
                // user history entry
                holder.avatar.setVisibility(View.GONE);
                holder.info.setText(time);
            } else {
                // friend history entry
                holder.avatar.setVisibility(View.VISIBLE);
                holder.info.setText(TextTools.dotSeparate(item.username, time));

                // trakt avatar
                ServiceUtils.loadWithPicasso(getContext(), item.avatar).into(holder.avatar);
            }

            // a TVDb or no poster
            TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster,
                    item.tvdbPosterUrl);

            holder.show.setText(item.title);
            holder.episode.setText(item.description);

            // action type indicator (only if showing trakt history)
            if (TRAKT_ACTION_WATCH.equals(item.action)) {
                holder.type.setImageDrawable(getDrawableWatched());
                holder.type.setVisibility(View.VISIBLE);
            } else if (item.action != null) {
                // check-in, scrobble
                holder.type.setImageDrawable(getDrawableCheckin());
                holder.type.setVisibility(View.VISIBLE);
            } else {
                holder.type.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        int itemType = getItem(position).type;
        switch (itemType) {
            case ItemType.HISTORY:
            case ItemType.FRIEND:
                return ViewType.HISTORY;
            case ItemType.MORE_LINK:
                return ViewType.MORE_LINK;
            case ItemType.HEADER:
                return ViewType.HEADER;
        }
        return 0;
    }

    protected Context getContext() {
        return context;
    }

    protected VectorDrawableCompat getDrawableWatched() {
        return drawableWatched;
    }

    protected VectorDrawableCompat getDrawableCheckin() {
        return drawableCheckin;
    }

    public NowItem getItem(int position) {
        return dataset.get(position);
    }

    public void setRecentlyWatched(List<NowItem> items) {
        int oldCount = recentlyWatched == null ? 0 : recentlyWatched.size();
        int newCount = items == null ? 0 : items.size();

        recentlyWatched = items;
        reloadData();
        notifyAboutChanges(0, oldCount, newCount);
    }

    public void setFriendsRecentlyWatched(List<NowItem> items) {
        int oldCount = friendsRecently == null ? 0 : friendsRecently.size();
        int newCount = items == null ? 0 : items.size();
        // items start after recently watched (if any)
        int startPosition = recentlyWatched == null ? 0 : recentlyWatched.size();

        friendsRecently = items;
        reloadData();
        notifyAboutChanges(startPosition, oldCount, newCount);
    }

    private void reloadData() {
        dataset.clear();
        if (recentlyWatched != null) {
            dataset.addAll(recentlyWatched);
        }
        if (friendsRecently != null) {
            dataset.addAll(friendsRecently);
        }
    }

    private void notifyAboutChanges(int startPosition, int oldItemCount, int newItemCount) {
        if (newItemCount == 0 && oldItemCount == 0) {
            return;
        }

        if (newItemCount == oldItemCount) {
            // identical number of items
            notifyItemRangeChanged(startPosition, oldItemCount);
        } else if (newItemCount > oldItemCount) {
            // more items than before
            if (oldItemCount > 0) {
                notifyItemRangeChanged(startPosition, oldItemCount);
            }
            notifyItemRangeInserted(startPosition + oldItemCount,
                    newItemCount - oldItemCount);
        } else {
            // less items than before
            if (newItemCount > 0) {
                notifyItemRangeChanged(startPosition, newItemCount);
            }
            notifyItemRangeRemoved(startPosition + newItemCount,
                    oldItemCount - newItemCount);
        }
    }
}
