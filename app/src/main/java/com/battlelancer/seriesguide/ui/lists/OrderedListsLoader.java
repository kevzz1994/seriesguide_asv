package com.battlelancer.seriesguide.ui.lists;

import android.content.Context;
import android.database.Cursor;

import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.androidutils.GenericSimpleLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads all user-created lists from the database into a list.
 */
class OrderedListsLoader extends GenericSimpleLoader<List<OrderedListsLoader.OrderedList>> {

    static class OrderedList {

        private String id;
        private String name;

        OrderedList(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    OrderedListsLoader(Context context) {
        super(context);
    }

    @Override
    public List<OrderedList> loadInBackground() {
        List<OrderedList> items = new ArrayList<>();

        Cursor query = getContext().getContentResolver()
                .query(SeriesGuideContract.Lists.CONTENT_URI,
                        ListsQuery.PROJECTION, null, null,
                        SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        if (query == null) {
            return items;
        }

        while (query.moveToNext()) {
            items.add(new OrderedList(
                    query.getString(ListsQuery.ID),
                    query.getString(ListsQuery.NAME)
            ));
        }

        query.close();

        return items;
    }

    private static class ListsQuery {
        static String[] PROJECTION = new String[]{
                SeriesGuideContract.Lists._ID,
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME
        };

        static int ID = 1;
        static int NAME = 2;
    }
}
