package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.File;

@Entity(tableName = "song_table")
public class SongDB {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "filepath")
    private String filepath;

    @NonNull
    @ColumnInfo(name = "playOrder")
    private int playOrder;

    @NonNull
    @ColumnInfo(name = "playlistContainerId")
    private int playlistContainerId;

    public SongDB(@NonNull String name, @NonNull String filepath, int playOrder, int playlistContainerId){
        this.name = name;
        this.filepath = filepath;
        this.playOrder = playOrder;
        this.playlistContainerId = playlistContainerId;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return new File(filepath);
    }

    public String getFilepath() {
        return filepath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(int playOrder) {
        this.playOrder = playOrder;
    }

    public int getPlaylistContainerId() {
        return playlistContainerId;
    }

    public void setPlaylistContainerId(int playlistContainerId) {
        this.playlistContainerId = playlistContainerId;
    }

    @SuppressLint("DefaultLocale")
    public String getStringDuration(){
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(getFile().getAbsolutePath());
        String out = "";
        String duration =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long dur = Long.parseLong(duration);
        long seconds = (dur % 60000) / 1000;

        long minutes = dur / 60000;
        out = minutes + ":" + seconds;
        Log.v("all time", out);
        // close object
        metaRetriever.release();
        return String.format("%d:%02d", minutes, seconds);
    }

    public int getDuration() {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(getFile().getAbsolutePath());
        String out = "";
        String duration =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        metaRetriever.release();
        return Integer.parseInt(duration);
    }

    public void setFilepath(String songNewFilepath) {
        filepath = songNewFilepath;
    }

    public void setName(String newName) {
        name = newName;
    }
}
