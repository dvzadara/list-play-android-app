package com.ListPlay.listplay;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class PlaylistsViewModel extends AndroidViewModel {
    private final PlaylistSongDao mPlaylistSongDao;
    private final File playlistsDir;

    public PlaylistsViewModel (Application application) {
        super(application);
        PlaylistSongRoomDatabase db = PlaylistSongRoomDatabase.getDatabase(application);
        mPlaylistSongDao = db.playlistSongDao();
        playlistsDir = application.getFilesDir();
    }

    LiveData<List<PlaylistWithSongsDB>> getLivePlaylistsWithSongs() {
        return mPlaylistSongDao.getLivePlaylistsWithSongs();
    }

    LiveData<PlaylistWithSongsDB> getSongsFromPlaylist(String playlistName)
    {
        return mPlaylistSongDao.getSongsFromPlaylist(playlistName);
    }

    LiveData<PlaylistWithSongsDB> getSongsFromPlaylist(int playlistId)
    {
        return mPlaylistSongDao.getSongsFromPlaylist(playlistId);
    }

    public PlaylistDB getPlaylistById(int playlistId) {
        return mPlaylistSongDao.getPlaylistById(playlistId);
    }

    public long insertPlaylist(PlaylistDB playlist) throws ExecutionException, InterruptedException {
        class MyInfoCallable implements Callable<Long> {
            final long i;
            public MyInfoCallable(PlaylistDB playlist) {
                this.i = mPlaylistSongDao.insertPlaylist(playlist);
            }
            @Override
            public Long call() throws Exception {
                return i;
            }
        }

        final long id = PlaylistSongRoomDatabase.databaseWriteExecutor.submit(new MyInfoCallable(playlist)).get();
        return id;
    }

    public void updatePlaylist(PlaylistDB playlist) {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.updatePlaylist(playlist);
        });
    }

    public void updatePlaylists(List<PlaylistDB> playlists) {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.updatePlaylists(playlists);
        });
    }

    public void insertSong(SongDB song) {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.insertSong(song);
        });
    }

    public int getMaxPlaylistOrder() {
        return mPlaylistSongDao.getMaxPlaylistOrder();
    }

    public void deletePlaylistWithSongs(PlaylistDB playlist, List<SongDB> songs){
        boolean allSongsDeleted = true;
        File playlistDir = new File(playlistsDir, "" + playlist.getId());
        for (SongDB song:songs) {
            if(song.getFile().delete()) {
                PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
                    mPlaylistSongDao.deleteSong(song);
                });
                Log.d("PlaylistsViewModel.deletePlaylistWithSongs", song.getFile().getName() + " is deleted");
            } else {
                Log.d("PlaylistsViewModel.deletePlaylistWithSongs", song.getFile().getAbsolutePath() + " is not deleted");
            }
        }
        for(File file:playlistDir.listFiles()) {
            if (!file.delete()) {
                allSongsDeleted = false;
                Log.d("PlaylistViewModel.deletePlaylistWithSongs", "can`t delete" + file.getAbsolutePath());
            }
        }
        if (allSongsDeleted) {
            if (!playlistDir.delete()) {
                allSongsDeleted = false;
                Log.d("PlaylistViewModel.deletePlaylistWithSongs", "can`t delete" + playlistDir.getAbsolutePath());
            }
        }
        if (allSongsDeleted) {
            PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
                mPlaylistSongDao.deletePlaylist(playlist);
            });
        }
    }

    public void renamePlaylist(PlaylistWithSongsDB playlist, String newName){
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            playlist.playlist.setName(newName);
            mPlaylistSongDao.updatePlaylist(playlist.playlist);
            mPlaylistSongDao.updateSongs(playlist.songs);
        });
    }

    public void updateSongs(List<SongDB> songs)
    {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.updateSongs(songs);
        });
    }

    public void updateSong(SongDB song) {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.updateSong(song);
        });
    }

    public void renameSong(SongDB song, String newName) {
        File oldSongFile = song.getFile();
        File renamedSongFile = new File(oldSongFile.getParentFile(), YoutubeAudioDownloader.fileNameNormalize(newName) + ".mp3");
        if (oldSongFile.renameTo(renamedSongFile)) {
            song.setName(newName);
            song.setFilepath(renamedSongFile.getAbsolutePath());
            PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
                mPlaylistSongDao.updateSong(song);
            });
        }
    }

    public void deleteSong(SongDB song) {
        PlaylistSongRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPlaylistSongDao.deleteSong(song);
        });
    }

    public int getMaxSongOrder(int playlistId) {
        return mPlaylistSongDao.getMaxSongOrder(playlistId);
    }
}
