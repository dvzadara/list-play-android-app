package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

interface MusicPlayerInterface{
    public void updateSongInfo(SongDB song);
    public void updatePlayingTime(MediaPlayer mediaPlayer);
    public void updateButtons(boolean playPauseButtonStatus, boolean playPreviousButtonIsEnabled, boolean playNextButtonIsEnabled);
    public void hidePlayer();
}

public class MusicPlayer {
    private static volatile MusicPlayer instance;
    private final File musicPlayerDir;
    MediaPlayer mediaPlayer;
    int playlistId = -1;
    String playlistName = "";
    SongDB[] songs;
    MusicPlayerInterface musicPlayerInterface;
    LiveData<PlaylistWithSongsDB> playlistLiveData;
    Observer<PlaylistWithSongsDB> playlistObserver;
    PlaylistsViewModel playlistsViewModel;
    int currentSong;

    private MusicPlayer(MusicPlayerInterface musicPlayerInterface, PlaylistsViewModel playlistsViewModel, File musicPlayerDir) {
        mediaPlayer = new MediaPlayer();
        this.musicPlayerInterface = musicPlayerInterface;
        this.musicPlayerInterface.updatePlayingTime(mediaPlayer);
        this.playlistsViewModel = playlistsViewModel;
        this.musicPlayerDir = musicPlayerDir;
        musicPlayerDir.mkdir();
        playlistObserver = new Observer<PlaylistWithSongsDB>() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onChanged(PlaylistWithSongsDB playlist) {
                if ((playlist != null) && (currentSong != -1)) {
                    playlist.songs.sort((song1, song2) -> {
                        return song1.getPlayOrder() - song2.getPlayOrder();
                    });
                    int currentSongId = songs[currentSong].getId();
                    songs = playlist.songs.toArray(new SongDB[0]);
                    currentSong = -1;
                    for (int i = 0; i < playlist.songs.size(); i++) {
                        if (songs[i].getId() == currentSongId) {
                            currentSong = i;
                        }
                    }
                    for (SongDB song : playlist.songs)
                        Log.d("MusicPlayer.observer", song.getName() + " " + song.getPlayOrder());
//                    Log.d("MusicPlayer.observer", songs[currentSong].getName() + " " + currentSong);
//                    if (currentSong == -1) {
//                        musicPlayerInterface.hidePlayer();
//                        stopCurrentSong();
//                    }
                    if (currentSong == -1) {
                        int playlistId = -1;
                        mediaPlayer.setOnCompletionListener(null);
                        stopCurrentSong();
                        musicPlayerInterface.hidePlayer();
                    } else {
                        setNextSong();
                        musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
                    }
                } else {
                    Log.d("MusicPlayer.observer", "playlist is null");
                    if (playlist != null)
                        mediaPlayer.setOnCompletionListener(null);
                        stopCurrentSong();
                        musicPlayerInterface.hidePlayer();
                }
            }
        };
    }

    public static MusicPlayer getInstance(
            MusicPlayerInterface musicPlayerInterface,
            PlaylistsViewModel playlistsViewModel,
            File musicPlayerDir
    ) {
        MusicPlayer localInstance = instance;
        if (localInstance == null) {
            synchronized (MusicPlayer.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MusicPlayer(musicPlayerInterface, playlistsViewModel, musicPlayerDir);
                }
            }
        }
        return localInstance;
    }

    public void startPlayingPlaylist(PlaylistWithSongsDB playlist) throws IOException {
        if (playlist.songs.size() > 0) {
            setPlaylist(playlist, 0);
            playCurrentSong();
            setNextSong();
        }
    }

    public void startPlayingPlaylist(PlaylistWithSongsDB playlist, SongDB currentSong) throws IOException {
        if (playlist.songs.size() > 0) {
            setPlaylist(playlist, getSongNumber(playlist, currentSong));
            playCurrentSong();
            setNextSong();
        }
    }

    public int getSongNumber(PlaylistWithSongsDB playlist, SongDB song) {
        for (int i = 0; i < playlist.songs.size(); i++)
            if (playlist.songs.get(i).getId() == song.getId())
                return i;
        return -1;
    }

    public void setPlaylist(PlaylistWithSongsDB playlist, int currentSong){
        playlistName = playlist.playlist.getName();
        playlistId = playlist.playlist.getId();
        playlist.songs.sort((song1, song2) -> {
            return song1.getPlayOrder() - song2.getPlayOrder();
        });
        songs = playlist.songs.toArray(new SongDB[0]);
        for(SongDB song:songs) {
            Log.d("MusicPlayer.setPlaylist", song.getName() + " " + song.getPlayOrder());
        }
        Log.d("MusicPlayer.setPlaylist", "" + currentSong);
        setObserver();
        this.currentSong = currentSong;
    }

    public void setObserver(){
        if (playlistLiveData != null)
            playlistLiveData.removeObserver(playlistObserver);
        playlistLiveData = playlistsViewModel.getSongsFromPlaylist(playlistId);
        playlistLiveData.observeForever(playlistObserver);
    }

    public void playCurrentSong() throws IOException {
        stopCurrentSong();
        if (songs[currentSong].getFile().exists())
        {
            musicPlayerInterface.updateSongInfo(songs[currentSong]);
            startPlayingFile(songs[currentSong].getFile());
            musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
        } else
            stopCurrentSong();
    }

    public void startPlayingFile(File file) throws IOException {
        File fileCopy = createCopy(file);
        mediaPlayer.setDataSource(fileCopy.getAbsolutePath());
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    public File createCopy(File file) {
        File fileCopy = new File(musicPlayerDir, file.getName());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(file.toPath(), fileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return fileCopy;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setNextSong(){
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (!currentSongIsLast())
                {
                    mediaPlayer.reset();
                    for (File file:musicPlayerDir.listFiles())
                        file.delete();

                    currentSong += 1;
                    try {
                        playCurrentSong();
                    } catch (IOException ignored) { }
                    setNextSong();
                } else {
                    mediaPlayer.setOnCompletionListener(null);
                    stopCurrentSong();
                    int playlistId = -1;
                    currentSong = -1;
                    musicPlayerInterface.hidePlayer();
                }
            }
        });
    }

    public void playPause(){
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause();
        else
            mediaPlayer.start();
        musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
    }

    public void play(){
        mediaPlayer.start();
        musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
    }

    public void pause(){
        mediaPlayer.start();
        musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
    }

    public void playNext() throws IOException {
        if (!currentSongIsLast()) {
            for (File file:musicPlayerDir.listFiles())
                file.delete();
            stopCurrentSong();
            currentSong += 1;
            playCurrentSong();
            setNextSong();
            musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
        }
    }

    public void playPrevious() throws IOException {
        if (!currentSongIsFirst()) {
            for (File file:musicPlayerDir.listFiles())
                file.delete();
            stopCurrentSong();
            currentSong -= 1;
            playCurrentSong();
            setNextSong();
            musicPlayerInterface.updateButtons(mediaPlayer.isPlaying(), currentSongIsFirst(), currentSongIsLast());
        }
    }
    public void changeTime(int time){
        Log.d("MusicPlayer.changeTime", "is called");
        mediaPlayer.seekTo(time);
    }

    public void stopCurrentSong(){
        for (File file:musicPlayerDir.listFiles())
            file.delete();
        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    public boolean currentSongIsLast(){
        return (currentSong == songs.length - 1);
    }

    public boolean currentSongIsFirst(){
        return (currentSong == 0);
    }

    public SongDB getCurrentSong(){
        if ((currentSong != -1) && (songs != null))
            return songs[currentSong];
        else
            return null;
    }
}
