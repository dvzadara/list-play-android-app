package com.ListPlay.listplay;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.List;

public class PlaylistsPage extends Fragment {
    private volatile PlaylistsViewModel mPlaylistsViewModel;

    public RecyclerView playlistsList;
    public ImageButton addPlaylistButton;
    private PlaylistPlayInterface playlistPlayInterface;
    private PlaylistSongsPageInterface playlistSongsPageInterface;
    private DownloadProgressInterface downloadProgressInterface;
    private PlaylistListAdapter myAdapter;

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

    public PlaylistsPage() {
        super(R.layout.fragment_playlists_page);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        playlistPlayInterface = (PlaylistPlayInterface) context;
        playlistSongsPageInterface = (PlaylistSongsPageInterface) context;
        downloadProgressInterface = (DownloadProgressInterface) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists_page, container, false);
        mPlaylistsViewModel = new ViewModelProvider(this).get(PlaylistsViewModel.class);

        addPlaylistButton = view.findViewById(R.id.addPlaylistButton);
        addPlaylistButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showAddPlaylistFragment(view);
            }
        });

        playlistsList = view.findViewById(R.id.playlistsView);

        myAdapter = new PlaylistListAdapter(playlistPlayInterface, playlistSongsPageInterface, mPlaylistsViewModel, downloadProgressInterface);
        updatePlayingPlaylist(playlistPlayInterface.getPlayingSong());
        updatePlayPause(playlistPlayInterface.getPlayPauseStatus());
        playlistsList.setAdapter(myAdapter);

        mPlaylistsViewModel.getLivePlaylistsWithSongs().observe(getViewLifecycleOwner(), new Observer<List<PlaylistWithSongsDB>>() {
            @Override
            public void onChanged(List<PlaylistWithSongsDB> playlistWithSongsDBS) {
                Log.d("PlaylistPage.playlistsObserver", String.valueOf(playlistWithSongsDBS.size()));
                myAdapter.updatePlaylistsList(playlistWithSongsDBS);
            }
        });

        ItemTouchHelper.Callback callback =
                new PlaylistItemTouchHelperCallback(myAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(playlistsList);

        playlistsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        playlistsList.removeItemDecorationAt(0);
        SpacesItemDecoration spacesDecor = new SpacesItemDecoration(12);
        playlistsList.addItemDecoration(spacesDecor);

        Log.d("PlaylistPage", "after playlistsList.setOnItemClickListener");

        return view;
    }

    public void showPlaylistSongsFragment(View view, PlaylistWithSongsDB playlist)
    {
        MainActivity mainActivity = ((MainActivity) requireActivity());
        mainActivity.setPlaylistSongsPage(playlist);
    }

    // Показ фрагмента для создания нового плейлиста
    public void showAddPlaylistFragment(View view)
    {
        MainActivity mainActivity = ((MainActivity) requireActivity());
        int downloadablePlaylistId;
        downloadablePlaylistId = mainActivity.getDownloadablePlaylistId();
        if (downloadablePlaylistId == -1) {
            Log.d("PlaylistPage.showAddPlaylistFragment", "mPlaylistsViewModel.downloadablePlaylistName is null");
            mainActivity.setPage(mainActivity.addPlaylistFromYoutubePage);
        } else {
            Log.d("PlaylistPage.showAddPlaylistFragment", "" + downloadablePlaylistId);
            mainActivity.showNotAvaibleActionMessage();
        }
    }

    void downloadPlaylistProgress(int songsDownload){
        Log.d("Thread:", Integer.toString(songsDownload));
    }

    // Всплывающий текст
    public void showInfoAlert(String s){
        AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistsPage.this.getContext());
        builder.setMessage(s);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void updatePlayingPlaylist(SongDB playingSong){
        myAdapter.updatePlayingPlaylist(playingSong);
    }

    public void updatePlayPause(boolean playPauseButtonStatus) {
        Log.d("PlaylistsPage.updatePlayPause", "" + playPauseButtonStatus);
        myAdapter.updatePlayPause(playPauseButtonStatus);
    }
}