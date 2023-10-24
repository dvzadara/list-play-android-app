package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PlaylistSongsPage extends Fragment {
    private PlaylistWithSongsDB playlist;
    public RecyclerView songsList;
    public TextView playlistName;
    public ImageButton addSongsButton;
    private PlaylistPlayInterface playlistPlayInterface;
    private DownloadProgressInterface downloadProgressInterface;
    private PlaylistsViewModel playlistsViewModel;
    private SongListAdapter myAdapter;
    private SpacesItemDecoration spaceDecor = new SpacesItemDecoration(12);

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space)
        {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
        {
            outRect.bottom = space;
        }
    }

    public PlaylistSongsPage(PlaylistWithSongsDB playlist, DownloadProgressInterface downloadProgressInterface,
                             PlaylistsViewModel playlistsViewModel) {
        super(R.layout.fragment_playlist_songs_page);
        this.downloadProgressInterface = downloadProgressInterface;
        this.playlistsViewModel = playlistsViewModel;
        this.playlist = playlist;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PlaylistPlayInterface) {
            playlistPlayInterface = (PlaylistPlayInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnButtonClickListener");
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists_page, container, false);

        return inflater.inflate(R.layout.fragment_playlist_songs_page, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        songsList = this.requireView().findViewById(R.id.playlistSongsView);
        addSongsButton = this.requireView().findViewById(R.id.addSongsToPlaylistButton);
        playlistName = this.requireView().findViewById(R.id.playlistSongsPageHeaderText);
        playlistName.setText(playlist.playlist.getName());

        addSongsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSongToPlaylistButtonClick();
            }
        });

        myAdapter = new SongListAdapter(playlistPlayInterface, playlistsViewModel, downloadProgressInterface);
        updatePlayingSong(playlistPlayInterface.getPlayingSong());
        updatePlayPause(playlistPlayInterface.getPlayPauseStatus());

        songsList.setAdapter(myAdapter);
        playlistsViewModel.getSongsFromPlaylist(playlist.playlist.getName()).observe(getViewLifecycleOwner(), new Observer<PlaylistWithSongsDB>() {
            @Override
            public void onChanged(PlaylistWithSongsDB playlistWithSongs) {
                Log.d("PlaylistPage.playlistsObserver", String.valueOf(playlistWithSongs.songs.size()));
                Log.d("PlaylistPage.playlistsObserver", playlist.playlist.getName());
                myAdapter.updatePlaylist(playlistWithSongs);
            }
        });

        ItemTouchHelper.Callback callback =
                new SongItemTouchHelperCallback(myAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(songsList);

        songsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        songsList.removeItemDecoration(spaceDecor);
        songsList.addItemDecoration(spaceDecor);
    }

    public void updatePlayingSong(SongDB playingSong){
        if (playingSong != null)
            Log.d("PlaylistSongsPage.updatePlayingSong", playingSong.getName());
        myAdapter.updatePlayingSong(playingSong);
    }

    public void updatePlayPause(boolean playPauseButtonStatus) {
        Log.d("PlaylistsPage.updatePlayPause", "" + playPauseButtonStatus);
        myAdapter.updatePlayPause(playPauseButtonStatus);
    }

    public void addSongToPlaylistButtonClick(){
        Log.d("PlaylistSongsPage.addSongToPlaylistButtonClick", "is called");
        MainActivity mainActivity = ((MainActivity) requireActivity());
        if (mainActivity.getDownloadablePlaylistId() == -1) {
            AlertDialog addSongsDialog = AddSongToPlaylistDialog.getAddSongToPlaylistDialog(mainActivity, playlist.playlist, playlistsViewModel);
            addSongsDialog.show();
        } else {
            mainActivity.showNotAvaibleActionMessage();
        }
    }
}