package com.battlelancer.seriesguide.ui.shows;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.widgets.WatchedBox;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Adapter for {@link CalendarFragment} with optimizations for image loading for smoother
 * scrolling.
 */
public class CalendarAdapter extends CursorAdapter implements StickyGridHeadersBaseAdapter {

    interface ItemClickListener {
        void onWatchedBoxClick(int episodePosition, boolean isWatched);
    }

    private final ItemClickListener itemClickListener;
    private final Calendar calendar;

    private List<HeaderData> headers;
    private boolean isShowingHeaders;

    CalendarAdapter(Activity activity, ItemClickListener itemClickListener) {
        super(activity, null, 0);
        this.itemClickListener = itemClickListener;
        this.calendar = Calendar.getInstance();
    }

    /**
     * Whether to show episodes grouped by day with header. Disable headers for larger data sets as
     * calculating them is expensive. Make sure to reload the data afterwards.
     */
    void setIsShowingHeaders(boolean isShowingHeaders) {
        this.isShowingHeaders = isShowingHeaders;
    }

    /**
     * Overrides base method and does proper position check before returning a Cursor.
     */
    @Override
    @Nullable
    public Cursor getItem(int position) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor;
        } else {
            return null;
        }
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.position = cursor.getPosition();

        // watched box
        int episodeFlag = cursor.getInt(CalendarQuery.WATCHED);
        viewHolder.watchedBox.setEpisodeFlag(episodeFlag);
        boolean watched = EpisodeTools.isWatched(episodeFlag);
        viewHolder.watchedBox.setContentDescription(
                context.getString(watched ? R.string.action_unwatched : R.string.action_watched));

        // show title
        viewHolder.show.setText(cursor.getString(CalendarQuery.SHOW_TITLE));

        // episode number and title
        final int season = cursor.getInt(CalendarQuery.SEASON);
        final int episode = cursor.getInt(CalendarQuery.NUMBER);
        boolean hideTitle = EpisodeTools.isUnwatched(episodeFlag)
                && DisplaySettings.preventSpoilers(context);
        viewHolder.episode.setText(TextTools.getNextEpisodeString(context, season, episode,
                hideTitle ? null : cursor.getString(CalendarQuery.TITLE)));

        // timestamp, absolute time and network
        long releaseTime = cursor.getLong(CalendarQuery.RELEASE_TIME_MS);
        String time = null;
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
            // timestamp
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
            viewHolder.timestamp.setText(displayExactDate ?
                    TimeTools.formatToLocalDateShort(context, actualRelease)
                    : TimeTools.formatToLocalRelativeTime(context, actualRelease));
            // release time of this episode
            time = TimeTools.formatToLocalTime(context, actualRelease);
        } else {
            viewHolder.timestamp.setText(null);
        }
        viewHolder.info.setText(TextTools.dotSeparate(cursor.getString(CalendarQuery.SHOW_NETWORK), time));

        // collected indicator
        boolean isCollected = EpisodeTools.isCollected(
                cursor.getInt(CalendarQuery.COLLECTED));
        viewHolder.collected.setVisibility(isCollected ? View.VISIBLE : View.GONE);

        // set poster
        TvdbImageTools.loadShowPosterResizeSmallCrop(context, viewHolder.poster,
                TvdbImageTools.smallSizeUrl(cursor.getString(CalendarQuery.SHOW_POSTER_PATH)));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar, parent, false);

        ViewHolder viewHolder = new ViewHolder(v, itemClickListener);
        v.setTag(viewHolder);

        return v;
    }

    private long getHeaderId(int position) {
        Cursor item = getItem(position);
        if (item != null) {
            /*
             * Map all episodes releasing the same day to the same id (which
             * equals the time midnight of their release day).
             */
            return getHeaderTime(item);
        }
        return 0;
    }

    private long getHeaderTime(Cursor item) {
        long releaseTime = item.getLong(CalendarQuery.RELEASE_TIME_MS);
        Date actualRelease = TimeTools.applyUserOffset(mContext, releaseTime);

        calendar.setTime(actualRelease);
        // not midnight because upcoming->recent is delayed 1 hour
        // so header would display wrong relative time close to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    @Override
    public int getCountForHeader(int position) {
        if (headers != null) {
            return headers.get(position).getCount();
        }
        return 0;
    }

    @Override
    public int getNumHeaders() {
        if (headers != null) {
            return headers.size();
        }
        return 0;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // get header position for item position
        position = headers.get(position).getRefPosition();

        Cursor item = getItem(position);
        if (item == null) {
            return null;
        }

        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grid_header, parent, false);

            holder = new HeaderViewHolder();
            holder.day = convertView.findViewById(R.id.textViewGridHeader);

            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        long headerTime = getHeaderTime(item);
        // display headers like "Mon in 3 days", also "today" when applicable
        holder.day.setText(
                TimeTools.formatToLocalDayAndRelativeWeek(mContext, new Date(headerTime)));

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        // re-create headers before letting notifyDataSetChanged reach the AdapterView
        headers = generateHeaderList();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // remove headers before letting notifyDataSetChanged reach the AdapterView
        headers = null;
        super.notifyDataSetInvalidated();
    }

    private List<HeaderData> generateHeaderList() {
        int count = getCount();
        if (count == 0 || !isShowingHeaders) {
            return null;
        }

        // pre-size to 30 as we display 30 days == headers at most
        LongSparseArray<HeaderData> mapping = new LongSparseArray<>(30);
        List<HeaderData> headers = new ArrayList<>();

        for (int position = 0; position < count; position++) {
            long headerId = getHeaderId(position);
            HeaderData headerData = mapping.get(headerId);
            if (headerData == null) {
                headerData = new HeaderData(position);
                headers.add(headerData);
            }
            headerData.incrementCount();
            mapping.put(headerId, headerData);
        }

        return headers;
    }

    static class ViewHolder {

        private TextView show;
        private TextView episode;
        private View collected;
        private WatchedBox watchedBox;
        private TextView info;
        private TextView timestamp;
        private ImageView poster;
        private int position;

        public ViewHolder(View v, final ItemClickListener itemClickListener) {
            show = v.findViewById(R.id.textViewActivityShow);
            episode = v.findViewById(R.id.textViewActivityEpisode);
            collected = v.findViewById(R.id.imageViewActivityCollected);
            watchedBox = v.findViewById(R.id.watchedBoxActivity);
            info = v.findViewById(R.id.textViewActivityInfo);
            timestamp = v.findViewById(R.id.textViewActivityTimestamp);
            poster = v.findViewById(R.id.imageViewActivityPoster);

            watchedBox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onWatchedBoxClick(position,
                                EpisodeTools.isWatched(watchedBox.getEpisodeFlag()));
                    }
                }
            });
            CheatSheet.setup(watchedBox);
        }

        public TextView getShow() {
            return show;
        }

        public void setShow(TextView show) {
            this.show = show;
        }

        public TextView getEpisode() {
            return episode;
        }

        public void setEpisode(TextView episode) {
            this.episode = episode;
        }

        public View getCollected() {
            return collected;
        }

        public void setCollected(View collected) {
            this.collected = collected;
        }

        public WatchedBox getWatchedBox() {
            return watchedBox;
        }

        public void setWatchedBox(WatchedBox watchedBox) {
            this.watchedBox = watchedBox;
        }

        public TextView getInfo() {
            return info;
        }

        public void setInfo(TextView info) {
            this.info = info;
        }

        public TextView getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(TextView timestamp) {
            this.timestamp = timestamp;
        }

        public ImageView getPoster() {
            return poster;
        }

        public void setPoster(ImageView poster) {
            this.poster = poster;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }

    static class HeaderViewHolder {

        private TextView day;

        public TextView getDay() {
            return day;
        }

        public void setDay(TextView day) {
            this.day = day;
        }
    }
}
