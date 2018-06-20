
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.gson.annotations.SerializedName;

public class ListItem {

    @SerializedName("list_item_id")
    public String listItemId;

    @SerializedName("tvdb_id")
    public int tvdbId;

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
