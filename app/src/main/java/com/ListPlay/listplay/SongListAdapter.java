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

class SongItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final SongListAdapter mAdapter;

    public SongItemTouchHelperCallback(SongListAdapter adapter) {
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

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder> {

    private PlaylistWithSongsDB playlist;
    private PlaylistPlayInterface playlistPlayInterface;
    private static DownloadProgressInterface downloadProgressInterface;
    private PlaylistsViewModel playlistsViewModel;
    private boolean itemSwaped = false;

    private boolean playPause = true;
    private int playingSongId = -1;

    private static final int VIEW_TYPE_PLAYING = 0;
    private static final int VIEW_TYPE_OTHER = 1;

    private ViewHolder2 playingSongViewHolder;

    public void onItemMove(int fromPosition, int toPosition) {

        Log.d("PlaylistListAdapter.onItemMove", fromPosition + " " + toPosition);
        Log.d("PlaylistListAdapter.onItemMove", "-------------");

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                int t = playlist.songs.get(i).getPlayOrder();
                playlist.songs.get(i).setPlayOrder(playlist.songs.get(i + 1).getPlayOrder());
                playlist.songs.get(i + 1).setPlayOrder(t);
                Collections.swap(playlist.songs, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                int t = playlist.songs.get(i).getPlayOrder();
                playlist.songs.get(i).setPlayOrder(playlist.songs.get(i - 1).getPlayOrder());
                playlist.songs.get(i - 1).setPlayOrder(t);
                Collections.swap(playlist.songs, i, i - 1);
            }
        }
        playlist.songs.sort((song1, song2) -> {
            return song1.getPlayOrder() - song2.getPlayOrder();
        });

        notifyItemMoved(fromPosition, toPosition);
        itemSwaped = true;
        for (SongDB song : playlist.songs) {
            Log.d("PlaylistListAdapter.onItemMove", song.getName() + " " + song.getPlayOrder());
        }
        playlistsViewModel.updateSongs(playlist.songs);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePlaylist(PlaylistWithSongsDB playlistWithSongsDB) {
        Log.d("SongListAdapter.updatePlaylist", "is called");
        playlist = playlistWithSongsDB;
        playlist.songs.sort((song1, song2) -> {
            return song1.getPlayOrder() - song2.getPlayOrder();
        });
        if (!itemSwaped) {
            notifyDataSetChanged();
            Log.d("SongListAdapter.updatePlaylist", "notifyDataSetChanged");
        } else {
            itemSwaped = false;
            Log.d("SongListAdapter.updatePlaylist", "itemSwaped = true");
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "SuspiciousIndentation"})
    public void updatePlayingSong(SongDB playingSong) {
        if (playingSong != null) {
            Log.d("SingListAdapter.updatePlayingSong", playingSong.getName());
            this.playingSongId = playingSong.getId();
        } else
            this.playingSongId = -1;
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

        public void bind(final PlaylistWithSongsDB playlist, SongDB song, PlaylistPlayInterface playlistPlayInterface) { }
    }

    public static class ViewHolder1 extends SongListAdapter.ViewHolder {
        private final TextView songName;
        private final TextView songDuration;
        private final ImageButton songPlayButton;
        private final ImageButton songMenuButton;
        private final PlaylistsViewModel playlistsViewModel;

        public ViewHolder1(View view, PlaylistsViewModel playlistsViewModel) {
            super(view);

            songName = view.findViewById(R.id.songName);
            songDuration = view.findViewById(R.id.songDuration);
            songPlayButton = view.findViewById(R.id.songPlayButton);
            songMenuButton = view.findViewById(R.id.songMenuButton);
            this.playlistsViewModel = playlistsViewModel;
        }

        @SuppressLint("SetTextI18n")
        public void bind(final PlaylistWithSongsDB playlist, SongDB song, PlaylistPlayInterface playlistPlayInterface) {
            Log.d("SongListAdapter.bind", song.getName());
            songName.setText(song.getName());
            songDuration.setText(song.getStringDuration());
            songPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("SongListAdapter.songPlayButton.click", "" + song.getName());
                    Log.d("SongListAdapter.songPlayButton.click", song.getName());
                    Log.d("SongListAdapter.songPlayButton.click", song.getName());
                    playlistPlayInterface.startPlayingPlaylist(playlist, song);
                }
            });

            songMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = SongPopupMenu.getMenu(view, song, playlistsViewModel, downloadProgressInterface);
                    popupMenu.show();
                }
            });
        }
    }

    public static class ViewHolder2 extends SongListAdapter.ViewHolder {
        private final TextView songName;
        private final TextView songDuration;
        private final ImageButton songPlayButton;
        private final ImageButton songMenuButton;
        private final PlaylistsViewModel playlistsViewModel;
        private final boolean playPauseStatus;

        public ViewHolder2(View view, PlaylistsViewModel playlistsViewModel, boolean playPauseStatus) {
            super(view);

            songName = view.findViewById(R.id.songName);
            songDuration = view.findViewById(R.id.songDuration);
            songPlayButton = view.findViewById(R.id.songPlayButton);
            songMenuButton = view.findViewById(R.id.songMenuButton);
            this.playlistsViewModel = playlistsViewModel;
            this.playPauseStatus = playPauseStatus;
        }

        @SuppressLint("SetTextI18n")
        public void bind(final PlaylistWithSongsDB playlist, SongDB song, PlaylistPlayInterface playlistPlayInterface) {
            Log.d("SongListAdapter.bind", song.getName());
            songName.setText(song.getName());
            songDuration.setText(song.getStringDuration());
            songPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playlistPlayInterface.playPause();
                }
            });

            songMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = SongPopupMenu.getMenu(view, song, playlistsViewModel, downloadProgressInterface);
                    popupMenu.show();
                }
            });
            updatePlayPause(playPauseStatus);
        }

        public void updatePlayPause(boolean playPauseStatus) {
            if (playPauseStatus) {
                songPlayButton.setImageResource(R.drawable.ic_baseline_pause_24);
                Log.d("PlaylistListAdapter.ViewHolder2.bind", "playPauseStatus " + playPauseStatus);
            } else {
                songPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                Log.d("PlaylistListAdapter.ViewHolder2.bind", "playPauseStatus " + playPauseStatus);
            }
        }
    }

    public SongListAdapter(PlaylistPlayInterface playlistPlayInterface,
                           PlaylistsViewModel playlistsViewModel, DownloadProgressInterface downloadProgressInterface) {
        PlaylistDB playlist = new PlaylistDB("", 0);
        List<SongDB> songs = new ArrayList<>();
        this.playlist = new PlaylistWithSongsDB(playlist, songs);
        this.playlistPlayInterface = playlistPlayInterface;
        this.playlistsViewModel = playlistsViewModel;
        this.downloadProgressInterface = downloadProgressInterface;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        if (viewType == VIEW_TYPE_PLAYING) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.playing_song_item, viewGroup, false);
            playingSongViewHolder = new SongListAdapter.ViewHolder2(view, playlistsViewModel, playPause);
            return playingSongViewHolder;
        } else {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.song_item, viewGroup, false);
            return new SongListAdapter.ViewHolder1(view, playlistsViewModel);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (playlist.songs.get(position).getId() == playingSongId) {
            return VIEW_TYPE_PLAYING;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        SongDB currentSong = playlist.songs.get(position);
        viewHolder.bind(playlist, currentSong, playlistPlayInterface);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return playlist.songs.size();
    }
}