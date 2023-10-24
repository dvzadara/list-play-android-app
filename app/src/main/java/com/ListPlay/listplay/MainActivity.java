package com.ListPlay.listplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


interface DownloadProgressInterface{
    public void startProgress(int maxProgress);
    public void showProgress(int currentProgress);
    public void endProgress(int maxProgress);
    public int getDownloadablePlaylistId();
    public void setDownloadablePlaylistId(int id);
    public void showNotAvaibleActionMessage();
}

interface PlaylistPlayInterface {
    void startPlayingPlaylist(PlaylistWithSongsDB playlist);
    void startPlayingPlaylist(PlaylistWithSongsDB playlist, SongDB currentSong);
    void playPause();
    SongDB getPlayingSong();
    boolean getPlayPauseStatus();
}

interface PlaylistSongsPageInterface{
    void setPlaylistSongsPage(PlaylistWithSongsDB currentPlaylist);
}

public class MainActivity extends AppCompatActivity
        implements DownloadProgressInterface, PlaylistPlayInterface, MusicPlayerInterface,
        PlaylistSongsPageInterface {

    PlaylistsPage playlistsPage = new PlaylistsPage();
    AddPlaylistFromYoutubePage addPlaylistFromYoutubePage = new AddPlaylistFromYoutubePage();
    PlaylistSongsPage playlistSongsPage;
    ProgressBar downloadProgressBar;
    ConstraintLayout musicPlayerLayout;
    ImageButton playPauseButton;
    ImageButton playPreviousSongButton;
    ImageButton playNextSongButton;
    SeekBar durationSeekBar;

    MusicPlayer musicPlayer;

    PlaylistSongRoomDatabase db;
    PlaylistSongDao playlistSongDao;
    PlaylistsViewModel playlistsViewModel;

    private volatile int downloadablePlaylistId = -1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = PlaylistSongRoomDatabase.getDatabase(this.getApplicationContext());
        playlistSongDao = db.playlistSongDao();
        playlistsViewModel = new PlaylistsViewModel(this.getApplication());
        musicPlayer = MusicPlayer.getInstance(this, playlistsViewModel, new File(getFilesDir(), ".MusicPlayerFiles"));

//        final Observer<List<PlaylistWithSongsDB>> nameObserver = new Observer<List<PlaylistWithSongsDB>>() {
//            @Override
//            public void onChanged(List<PlaylistWithSongsDB> playlistWithSongsDBS) {
//                for (PlaylistWithSongsDB playlist:playlistWithSongsDBS) {
//                    Log.d("MainActivity.playlistList", playlist.playlist.getName() + " " + playlist.songs.size() + " songs" + playlist.playlist.getId() + " id");
//                }
//            }
//        };

//        playlistsViewModel.getLivePlaylistsWithSongs().observe(this, nameObserver);

        downloadProgressBar = findViewById(R.id.progressBar);

        musicPlayerLayout = findViewById(R.id.musicPlayerLayout);
        playPauseButton = findViewById(R.id.playSongButton);
        playPreviousSongButton = findViewById(R.id.previousSongButton);
        playNextSongButton = findViewById(R.id.nextSongButton);
        durationSeekBar = findViewById(R.id.durationSeekBar);

        setPage(playlistsPage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishAffinity();
    }

    public void addPlaylistFromYoutube(String name, String playlistUrl){
        MainActivity mainActivity = this;

        Thread addPlaylistThread = new Thread() {
            @Override
            public void run() {
                Log.d("Thread:", "Start");
                File playlistsDir = getFilesDir();
                try {
                    YoutubeAudioDownloader.YoutubePlaylistToFiles(
                            playlistUrl,
                            name,
                            playlistsDir,
                            mainActivity,
                            playlistsViewModel);
//                    Log.d("MainActivity.addPlaylistFromYoutube", playlistSongDao.getPlaylists().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("Thread:", "End");
            }
        };
        addPlaylistThread.start();
    }

    public void setPage(Fragment page){
        FragmentTransaction tf = getSupportFragmentManager().beginTransaction();
        tf.replace(R.id.frame_layout, page);
        tf.addToBackStack(null);
        tf.commit();
    }

    public void setPlaylistSongsPage(PlaylistWithSongsDB currentPlaylist){
        playlistSongsPage = new PlaylistSongsPage(currentPlaylist, this, playlistsViewModel);
        setPage(playlistSongsPage);
    }

    @Override
    public void startProgress(int maxProgress) {
        Toast.makeText(this, maxProgress + " songs download started", Toast.LENGTH_SHORT).show();
        downloadProgressBar.setMax(maxProgress + 1);
        downloadProgressBar.setVisibility(ProgressBar.VISIBLE);
        downloadProgressBar.setProgress(1);
    }

    @Override
    public void showProgress(int currentProgress) {
        downloadProgressBar.setProgress(currentProgress + 1);
    }

    @Override
    public void endProgress(int maxProgress) {
        Toast.makeText(this, maxProgress + " songs downloaded", Toast.LENGTH_SHORT).show();
        downloadProgressBar.setVisibility(ProgressBar.INVISIBLE);
//        playlistsPage.playlistsUpdate();
    }

    @Override
    public int getDownloadablePlaylistId() {
        return downloadablePlaylistId;
    }

    @Override
    public void setDownloadablePlaylistId(int id) {
        downloadablePlaylistId = id;
    }

    @Override
    public void showNotAvaibleActionMessage() {
        Toast.makeText(this, "Wait for the end of the current download", Toast.LENGTH_SHORT).show();
    }

    public void startPlayingPlaylist(PlaylistWithSongsDB playlist){
        showMusicPlayer();
        try {
            Log.d("MainActivity.startPlayingPlaylist", "is called");
            musicPlayer.startPlayingPlaylist(playlist);
            durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (b)
                        musicPlayer.changeTime(i);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    musicPlayer.pause();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    musicPlayer.play();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startPlayingPlaylist(PlaylistWithSongsDB playlist, SongDB currentSong) {
        showMusicPlayer();
        try {
            musicPlayer.startPlayingPlaylist(playlist, currentSong);
            durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (b)
                        musicPlayer.changeTime(i);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    musicPlayer.pause();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    musicPlayer.play();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playPause() {
        musicPlayer.playPause();
    }

    @Override
    public SongDB getPlayingSong() {
        return musicPlayer.getCurrentSong();
    }

    @Override
    public boolean getPlayPauseStatus() {
        return musicPlayer.mediaPlayer.isPlaying();
    }

    public void showMusicPlayer(){
        musicPlayerLayout.setVisibility(View.VISIBLE);
        durationSeekBar.setVisibility(View.VISIBLE);

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicPlayer.playPause();
            }
        });

        playPreviousSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    musicPlayer.playPrevious();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                playPreviousSongButton.setEnabled(!musicPlayer.currentSongIsFirst());
            }
        });

        playNextSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    musicPlayer.playNext();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                playPreviousSongButton.setEnabled(!musicPlayer.currentSongIsLast());
            }
        });
    }

    @Override
    public void updateSongInfo(SongDB song) {
        if (song != null) {
            Log.d("MainActivity.updateSongInfo", "is called");
            playlistsPage.updatePlayingPlaylist(musicPlayer.getCurrentSong());
            if (playlistSongsPage != null) {
                playlistSongsPage.updatePlayingSong(musicPlayer.getCurrentSong());
            }

            TextView songNameTextView = (TextView) findViewById(R.id.marqueeSongName);
            songNameTextView.setSelected(false);
            songNameTextView.setText(song.getName());
            songNameTextView.setSelected(true);

            TextView totalDurationTextView = (TextView) findViewById(R.id.playedSongTotalDuration);
            totalDurationTextView.setText(song.getStringDuration());
            int totalDuration = song.getDuration();
            durationSeekBar.setMax(totalDuration);
        }
    }

    @Override
    public void updatePlayingTime(MediaPlayer mediaPlayer) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentDuration = mediaPlayer.getCurrentPosition();
                    TextView currentDurationText = findViewById(R.id.playedSongCurrentDuration);
                    int minutes = (currentDuration / 1000) / 60;
                    int seconds = (currentDuration / 1000) % 60;
                    String durationString = String.format("%d:%02d", minutes, seconds);
                    currentDurationText.setText(durationString);
                    durationSeekBar.setProgress(currentDuration);
//                    Log.d("MainActivity.updatePlayingTime", "current_progress: " + currentDuration);
                }
                handler.postDelayed(this, 80);
            }
        }, 80);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void updateButtons(boolean playPauseButtonStatus, boolean playPreviousButtonIsEnabled, boolean playNextButtonIsEnabled) {
        playPreviousSongButton.setEnabled(!playPreviousButtonIsEnabled);
        playNextSongButton.setEnabled(!playNextButtonIsEnabled);
        if (playPauseButtonStatus)
            playPauseButton.setImageResource(R.drawable.pause_button_img);
        else
            playPauseButton.setImageResource(R.drawable.play_button_img);
        playlistsPage.updatePlayPause(playPauseButtonStatus);
        if (playlistSongsPage != null) {
            playlistSongsPage.updatePlayPause(playPauseButtonStatus);
        }
    }

    @Override
    public void hidePlayer() {
        musicPlayerLayout.setVisibility(View.GONE);
        durationSeekBar.setVisibility(View.GONE);
        playlistsPage.updatePlayPause(false);
        if (playlistSongsPage != null) {
            playlistSongsPage.updatePlayPause(false);
        }
        playlistsPage.updatePlayingPlaylist(musicPlayer.getCurrentSong());
        if (playlistSongsPage != null) {
            playlistSongsPage.updatePlayingSong(musicPlayer.getCurrentSong());
        }
    }
}