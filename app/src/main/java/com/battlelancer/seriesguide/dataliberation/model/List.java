
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class List {

    @SerializedName("list_id")
    public String listId;
    private String name;
    private int order;

    private java.util.List<ListItem> items = new ArrayList<>();

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public java.util.List<ListItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<ListItem> items) {
        this.items = items;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, listId);
        values.put(Lists.NAME, name);
        values.put(Lists.ORDER, order);
        return values;
    }
}
