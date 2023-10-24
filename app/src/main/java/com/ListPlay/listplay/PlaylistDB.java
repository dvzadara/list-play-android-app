package com.ListPlay.listplay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_table", indices = {@Index(value = {"name"}, unique = true)})
public class PlaylistDB {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "playOrder")
    private int playOrder;

    public PlaylistDB(@NonNull String name, int playOrder) {
        this.name = name;
        this.playOrder = playOrder;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(int playOrder) {
        this.playOrder = playOrder;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

class PlaylistOrder {
    private int id;
    private String name;
    private int playOrder;

    public String getName() {
        return name;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public int getId() {
        return id;
    }
}