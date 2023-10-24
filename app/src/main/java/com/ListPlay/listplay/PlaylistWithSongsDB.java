package com.ListPlay.listplay;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.Collections;
import java.util.List;

public class PlaylistWithSongsDB {
    @Embedded
    public PlaylistDB playlist;
    @Relation(
            parentColumn = "id",
            entityColumn = "playlistContainerId"
    )
    public List<SongDB> songs;

    PlaylistWithSongsDB(PlaylistDB playlist, List<SongDB> songs)
    {
        this.playlist = playlist;
        this.songs = songs;
    }

    public PlaylistDB getPlaylist() {
        return playlist;
    }
}
