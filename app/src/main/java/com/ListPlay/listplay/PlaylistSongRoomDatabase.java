package com.ListPlay.listplay;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {PlaylistDB.class, SongDB.class}, version = 10, exportSchema = false)
public abstract class PlaylistSongRoomDatabase extends RoomDatabase {

    public abstract PlaylistSongDao playlistSongDao();

    private static volatile PlaylistSongRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PlaylistSongRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PlaylistSongRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                                    PlaylistSongRoomDatabase.class, "playlists_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
