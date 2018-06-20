package com.battlelancer.seriesguide.ui.lists;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * Adapter for a list in the Lists section.
 */
class ListItemsAdapter extends BaseShowsAdapter {

    ListItemsAdapter(Activity activity, OnItemClickListener listener) {
        super(activity, listener);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ListItemViewHolder viewHolder = (ListItemViewHolder) view.getTag();

        viewHolder.setShowTvdbId(cursor.getInt(Query.SHOW_ID));
        viewHolder.setFavorited(cursor.getInt(Query.SHOW_FAVORITE) == 1);

        // show title
        viewHolder.getName().setText(cursor.getString(Query.SHOW_TITLE));

        // favorite label
        setFavoriteState(viewHolder.getFavorited(), viewHolder.isFavorited());

        // item title
        final int itemType = cursor.getInt(Query.ITEM_TYPE);
        switch (itemType) {
            default:
            case 1:
                // shows
                int time = cursor.getInt(Query.SHOW_OR_EPISODE_RELEASE_TIME);
                int weekDay = cursor.getInt(Query.SHOW_RELEASE_WEEKDAY);
                String timeZone = cursor.getString(Query.SHOW_RELEASE_TIMEZONE);
                String country = cursor.getString(Query.SHOW_RELEASE_COUNTRY);
                String network = cursor.getString(Query.SHOW_NETWORK);

                Date releaseTimeShow;
                if (time != -1) {
                    releaseTimeShow = TimeTools.getShowReleaseDateTime(context, time, weekDay,
                            timeZone, country, network);
                } else {
                    releaseTimeShow = null;
                }

                // network, regular day and time
                viewHolder.getTimeAndNetwork().setText(
                        TextTools.networkAndTime(context, releaseTimeShow, weekDay, network));

                // next episode info
                String fieldValue = cursor.getString(Query.SHOW_NEXTTEXT);
                if (TextUtils.isEmpty(fieldValue)) {
                    // display show status if there is no next episode
                    viewHolder.getEpisodeTime().setText(ShowTools.getStatus(context,
                            cursor.getInt(Query.SHOW_STATUS)));
                    viewHolder.getEpisode().setText(null);
                } else {
                    viewHolder.getEpisode().setText(fieldValue);

                    Date releaseTimeEpisode = TimeTools.applyUserOffset(context,
                            cursor.getLong(Query.SHOW_NEXT_DATE_MS));
                    boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
                    String dateTime = displayExactDate ?
                            TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
                            : TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode);
                    if (TimeTools.isSameWeekDay(releaseTimeEpisode, releaseTimeShow, weekDay)) {
                        // just display date
                        viewHolder.getEpisodeTime().setText(dateTime);
                    } else {
                        // display date and explicitly day
                        viewHolder.getEpisodeTime().setText(
                                context.getString(R.string.format_date_and_day, dateTime,
                                        TimeTools.formatToLocalDay(releaseTimeEpisode)));
                    }
                }

                // remaining count
                setRemainingCount(viewHolder.getRemainingCount(),
                        cursor.getInt(Query.SHOW_UNWATCHED_COUNT));
                break;
            case 2:
                // seasons
                viewHolder.getTimeAndNetwork().setText(R.string.season);
                viewHolder.getEpisode().setText(SeasonTools.getSeasonString(context,
                        cursor.getInt(Query.ITEM_TITLE)));
                viewHolder.getEpisodeTime().setText(null);
                viewHolder.getRemainingCount().setVisibility(View.GONE);
                break;
            case 3:
                // episodes
                viewHolder.getTimeAndNetwork().setText(R.string.episode);
                viewHolder.getEpisode().setText(TextTools.getNextEpisodeString(context,
                        cursor.getInt(Query.SHOW_NEXTTEXT),
                        cursor.getInt(Query.SHOW_NEXTEPISODE_OR_EPISODE_NUMBER),
                        cursor.getString(Query.ITEM_TITLE)));
                long releaseTime = cursor.getLong(Query.SHOW_OR_EPISODE_RELEASE_TIME);
                if (releaseTime != -1) {
                    // "in 15 mins (Fri)"
                    Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
                    viewHolder.getEpisodeTime().setText(context.getString(
                            R.string.format_date_and_day,
                            TimeTools.formatToLocalRelativeTime(context, actualRelease),
                            TimeTools.formatToLocalDay(actualRelease)));
                }
                viewHolder.getRemainingCount().setVisibility(View.GONE);
                break;
        }

        // poster
        TvdbImageTools.loadShowPosterResizeCrop(context, viewHolder.getPoster(),
                cursor.getString(Query.SHOW_POSTER));

        // context menu
        viewHolder.itemType = itemType;
        viewHolder.itemId = cursor.getString(Query.LIST_ITEM_ID);
        viewHolder.itemTvdbId = cursor.getInt(Query.ITEM_REF_ID);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ListItemViewHolder viewHolder = new ListItemViewHolder(v, onItemClickListener);
        v.setTag(viewHolder);

        return v;
    }

    public static class ListItemViewHolder extends ShowViewHolder {

        private String itemId;
        private int itemTvdbId;
        private int itemType;

        public ListItemViewHolder(View v, OnItemClickListener onItemClickListener) {
            super(v, onItemClickListener);
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public int getItemTvdbId() {
            return itemTvdbId;
        }

        public void setItemTvdbId(int itemTvdbId) {
            this.itemTvdbId = itemTvdbId;
        }

        public int getItemType() {
            return itemType;
        }

        public void setItemType(int itemType) {
            this.itemType = itemType;
        }
    }

    public static class Query {

        static String[] PROJECTION = new String[] {
                ListItems._ID, // 0
                ListItems.LIST_ITEM_ID,
                ListItems.ITEM_REF_ID,
                ListItems.TYPE,
                Shows.REF_SHOW_ID,
                Shows.TITLE, // 5
                Shows.OVERVIEW,
                Shows.POSTER,
                Shows.NETWORK,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY, // 10
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.STATUS,
                Shows.NEXTTEXT,
                Shows.NEXTEPISODE, // 15
                Shows.NEXTAIRDATEMS,
                Shows.FAVORITE,
                Shows.UNWATCHED_COUNT // 18
        };

        static int LIST_ITEM_ID = 1;
        static int ITEM_REF_ID = 2;
        static int ITEM_TYPE = 3;
        static int SHOW_ID = 4;
        static int SHOW_TITLE = 5;
        static int ITEM_TITLE = 6;
        static int SHOW_POSTER = 7;
        static int SHOW_NETWORK = 8;
        static int SHOW_OR_EPISODE_RELEASE_TIME = 9;
        static int SHOW_RELEASE_WEEKDAY = 10;
        static int SHOW_RELEASE_TIMEZONE = 11;
        static int SHOW_RELEASE_COUNTRY = 12;
        static int SHOW_STATUS = 13;
        static int SHOW_NEXTTEXT = 14;
        static int SHOW_NEXTEPISODE_OR_EPISODE_NUMBER = 15;
        static int SHOW_NEXT_DATE_MS = 16;
        static int SHOW_FAVORITE = 17;
        static int SHOW_UNWATCHED_COUNT = 18;
    }
}
