package com.ListPlay.listplay;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlaylistSongDao {
    @Transaction
    @Query("SELECT * FROM playlist_table ORDER BY playOrder")
    public LiveData<List<PlaylistWithSongsDB>> getLivePlaylistsWithSongs();

    @Query("SELECT * FROM playlist_table playOrder")
    public List<PlaylistDB> getPlaylists();

    @Update(entity = PlaylistDB.class)
    public void updatePlaylistOrders(PlaylistOrder playlist1, PlaylistOrder playlist2);

    @Query("SELECT * FROM song_table")
    public List<SongDB> getSongs();

    @Transaction
    @Query("SELECT * FROM playlist_table WHERE name=:playlistName ORDER BY playOrder")
    public LiveData<PlaylistWithSongsDB> getSongsFromPlaylist(String playlistName);

    @Transaction
    @Query("SELECT * FROM playlist_table WHERE id=:playlistId")
    public LiveData<PlaylistWithSongsDB> getSongsFromPlaylist(int playlistId);

    @Query("SELECT * FROM playlist_table WHERE id=:playlistId")
    PlaylistDB getPlaylistById(int playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insertPlaylist(PlaylistDB playlist);

    @Update
    void updatePlaylist(PlaylistDB playlist);

    @Query("UPDATE playlist_table " +
            "SET name=:newName WHERE name=:oldName")
    void renamePlaylist(String oldName, String newName);

    @Update
    void updatePlaylistAndSongs(PlaylistDB playlist, List<SongDB> songs);

    @Update
    void updateSongs(List<SongDB> songs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertSong(SongDB song);

    @Query("SELECT max(playOrder) FROM playlist_table")
    int getMaxPlaylistOrder();

    @Update
    void updatePlaylists(List<PlaylistDB> playlists);

    @Update
    void updateSong(SongDB song);

    @Delete
    void deletePlaylist(PlaylistDB playlist);

    @Delete
    void deleteSong(SongDB song);

    @Query("SELECT max(playOrder) FROM song_table WHERE playlistContainerId=:playlistId")
    int getMaxSongOrder(int playlistId);
}
