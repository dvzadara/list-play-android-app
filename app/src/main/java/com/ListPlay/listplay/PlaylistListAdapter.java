package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class PlaylistItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final PlaylistListAdapter mAdapter;

    public PlaylistItemTouchHelperCallback(PlaylistListAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
}

public class PlaylistListAdapter extends RecyclerView.Adapter<PlaylistListAdapter.ViewHolder> {

    private List<PlaylistWithSongsDB> playlists;
    private PlaylistPlayInterface playlistPlayInterface;
    private PlaylistSongsPageInterface playlistSongsPageInterface;
    private static DownloadProgressInterface downloadProgressInterface;
    private PlaylistsViewModel playlistsViewModel;
    private boolean itemSwaped = false;
    private int playingPlaylistId = -1;

    private static final int VIEW_TYPE_PLAYING = 0;
    private static final int VIEW_TYPE_OTHER = 1;
    private boolean playPause = true;

    private ViewHolder2 playingSongViewHolder;

    public void onItemMove(int fromPosition, int toPosition) {
        Log.d("PlaylistListAdapter.onItemMove", fromPosition + " " + toPosition);
        for (PlaylistWithSongsDB playlist : playlists) {
            Log.d("PlaylistListAdapter.onItemMove", playlist.playlist.getName() + " " + playlist.playlist.getPlayOrder());
        }
        Log.d("PlaylistListAdapter.onItemMove", "-------------");

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
//                Collections.swap(playlists, i, i + 1);
                int t = playlists.get(i).playlist.getPlayOrder();
                playlists.get(i).playlist.setPlayOrder(playlists.get(i + 1).playlist.getPlayOrder());
                playlists.get(i + 1).playlist.setPlayOrder(t);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
//                Collections.swap(playlists, i, i - 1);
                int t = playlists.get(i).playlist.getPlayOrder();
                playlists.get(i).playlist.setPlayOrder(playlists.get(i - 1).playlist.getPlayOrder());
                playlists.get(i - 1).playlist.setPlayOrder(t);
            }
        }
        playlists.sort((playlist1, playlist2) -> {
            return playlist1.playlist.getPlayOrder() - playlist2.playlist.getPlayOrder();
        });

        notifyItemMoved(fromPosition, toPosition);
        itemSwaped = true;
        List<PlaylistDB> onlyPlaylists = playlists.stream()
                .map(PlaylistWithSongsDB::getPlaylist)
                .collect(Collectors.toList());
        for (PlaylistWithSongsDB playlist : playlists) {
            Log.d("PlaylistListAdapter.onItemMove", playlist.playlist.getName() + " " + playlist.playlist.getPlayOrder());
        }
        playlistsViewModel.updatePlaylists(onlyPlaylists);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePlaylistsList(List<PlaylistWithSongsDB> playlistWithSongsDBS) {
        Log.d("PlaylistListAdapter.updatePlaylistsList", "is called");
        playlists = playlistWithSongsDBS;
        if (!itemSwaped) {
            notifyDataSetChanged();
        } else {
            itemSwaped = false;
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "SuspiciousIndentation"})
    public void updatePlayingPlaylist(SongDB playingSong) {
        if (playingSong != null)
            this.playingPlaylistId = playingSong.getPlaylistContainerId();
        else
            this.playingPlaylistId = -1;
        notifyDataSetChanged();
    }

    public void updatePlayPause(boolean playPause){
        this.playPause = playPause;
        if (playingSongViewHolder != null)
            playingSongViewHolder.updatePlayPause(playPause);
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(final PlaylistWithSongsDB playlist,
                         final PlaylistSongsPageInterface playlistSongsPageInterface,
                         PlaylistPlayInterface playlistPlayInterface) { }
    }

    public static class ViewHolder1 extends PlaylistListAdapter.ViewHolder {
        private final TextView playlistName;
        private final TextView numOfSongs;
        private final ImageButton playlistPlayButton;
        private final ImageButton playlistMenuButton;
        private final PlaylistsViewModel playlistsViewModel;

        public ViewHolder1(View view, PlaylistsViewModel playlistsViewModel) {
            super(view);

            playlistName = view.findViewById(R.id.playlistName);
            numOfSongs = view.findViewById(R.id.numOfSongs);
            playlistPlayButton = view.findViewById(R.id.playlistPlayButton);
            playlistMenuButton = view.findViewById(R.id.playlistItemMenuButton);
            this.playlistsViewModel = playlistsViewModel;
        }

        public void bind(final PlaylistWithSongsDB playlist,
                         final PlaylistSongsPageInterface playlistSongsPageInterface,
                         PlaylistPlayInterface playlistPlayInterface) {
            playlistName.setText(playlist.playlist.getName());
            numOfSongs.setText(playlist.songs.size() + " songs");
            playlistPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playlistPlayInterface.startPlayingPlaylist(playlist);
                }
            });

            playlistMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = PlaylistPopupMenu.getMenu(view, playlist, playlistsViewModel, downloadProgressInterface);
                    popupMenu.show();
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    playlistSongsPageInterface.setPlaylistSongsPage(playlist);
                }
            });
        }
    }

    public static class ViewHolder2 extends PlaylistListAdapter.ViewHolder {
        private final TextView playlistName;
        private final TextView numOfSongs;
        private final ImageButton playlistPlayButton;
        private final ImageButton playlistMenuButton;
        private final PlaylistsViewModel playlistsViewModel;
        private final boolean playPauseStatus;

        public ViewHolder2(View view, PlaylistsViewModel playlistsViewModel, boolean playPauseStatus) {
            super(view);

            playlistName = view.findViewById(R.id.playlistName);
            numOfSongs = view.findViewById(R.id.numOfSongs);
            playlistPlayButton = view.findViewById(R.id.playlistPlayButton);
            playlistMenuButton = view.findViewById(R.id.playlistItemMenuButton);
            this.playlistsViewModel = playlistsViewModel;
            this.playPauseStatus = playPauseStatus;
        }

        public void bind(final PlaylistWithSongsDB playlist,
                         final PlaylistSongsPageInterface playlistSongsPageInterface,
                         PlaylistPlayInterface playlistPlayInterface) {
            playlistName.setText(playlist.playlist.getName());
            numOfSongs.setText(playlist.songs.size() + " songs");

            if (playPauseStatus == true)
                playlistPlayButton.setImageResource(R.drawable.ic_baseline_pause_24);
            else
                playlistPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);

            playlistPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playlistPlayInterface.playPause();
                }
            });

            playlistMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = PlaylistPopupMenu.getMenu(view, playlist, playlistsViewModel, downloadProgressInterface);
                    popupMenu.show();
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    playlistSongsPageInterface.setPlaylistSongsPage(playlist);
                }
            });
            updatePlayPause(playPauseStatus);
        }

        public void updatePlayPause(boolean playPauseStatus) {
            if (playPauseStatus == true) {
                playlistPlayButton.setImageResource(R.drawable.ic_baseline_pause_24);
                Log.d("PlaylistListAdapter.ViewHolder2.bind", "playPauseStatus " + playPauseStatus);
            } else {
                playlistPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                Log.d("PlaylistListAdapter.ViewHolder2.bind", "playPauseStatus " + playPauseStatus);
            }
        }
    }

    public PlaylistListAdapter(PlaylistPlayInterface playlistPlayInterface,
                               PlaylistSongsPageInterface playlistSongsPageInterface, PlaylistsViewModel playlistsViewModel,
                               DownloadProgressInterface downloadProgressInterface) {
        this.playlists = new ArrayList<>();
        this.playlistPlayInterface = playlistPlayInterface;
        this.playlistSongsPageInterface = playlistSongsPageInterface;
        this.playlistsViewModel = playlistsViewModel;
        this.downloadProgressInterface = downloadProgressInterface;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PlaylistListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        if (viewType == VIEW_TYPE_PLAYING) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.playing_playlist_item, viewGroup, false);
            playingSongViewHolder = new ViewHolder2(view, playlistsViewModel, playPause);
            return playingSongViewHolder;
        } else {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.playlist_item, viewGroup, false);
            return new ViewHolder1(view, playlistsViewModel);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (playlists.get(position).playlist.getId() == playingPlaylistId) {
            return VIEW_TYPE_PLAYING;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(PlaylistListAdapter.ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        PlaylistWithSongsDB currentPlaylist = playlists.get(position);
        viewHolder.bind(currentPlaylist, playlistSongsPageInterface, playlistPlayInterface);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return playlists.size();
    }
}