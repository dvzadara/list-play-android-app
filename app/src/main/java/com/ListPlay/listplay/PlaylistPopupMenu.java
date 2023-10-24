package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class PlaylistPopupMenu {
    public static PopupMenu getMenu(View view, PlaylistWithSongsDB playlist,
                                    PlaylistsViewModel playlistsViewModel, DownloadProgressInterface downloadProgressInterface){
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);

        popupMenu.getMenuInflater().inflate(R.menu.playlist_popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @SuppressLint("IntentReset")
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.share)
                {
                    ArrayList<Uri> uris = new ArrayList<>();

                    for (SongDB song:playlist.songs) {
                        File file = song.getFile();
                        uris.add(FileProvider.getUriForFile(view.getContext(), "com.mafarchik.listplay.fileprovider", file));
//                        uris.add(Uri.parse("file:" + song.getFilepath()));
                    }
                    Intent myIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    myIntent.setType("application/*");
                    myIntent.putExtra(Intent.EXTRA_STREAM, uris);
//                    myIntent.setData(uris.get(0));
//                    myIntent.putExtra(Intent.EXTRA_SUBJECT,
//                        "Sharing File...");
//                    myIntent.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    myIntent = Intent.createChooser(myIntent, "Share songs");
                    myIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    view.getContext().getApplicationContext().startActivity(myIntent);
                } else if (downloadProgressInterface.getDownloadablePlaylistId() == playlist.playlist.getId())
                    downloadProgressInterface.showNotAvaibleActionMessage();
                else if (menuItem.getItemId() == R.id.delete) {
                    AlertDialog deleteDialog = getDeletePlaylistDialog(view, playlist, playlistsViewModel);
                    deleteDialog.show();
                } else if (menuItem.getItemId() == R.id.rename) {
                    AlertDialog renameDialog = getRenamePlaylistDialog(view, playlist, playlistsViewModel);
                    renameDialog.show();
                }
                Log.d("PlaylistPage.playlistMenuButton", "button: " + menuItem.getTitle());
                return true;
            }
        });
        return popupMenu;
    }

    public static AlertDialog getDeletePlaylistDialog(View view, PlaylistWithSongsDB playlist, PlaylistsViewModel playlistsViewModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                playlistsViewModel.deletePlaylistWithSongs(playlist.playlist, playlist.songs);
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });
        builder.setMessage("Delete " + playlist.playlist.getName() + "?");
        AlertDialog dialog = builder.create();
        return  dialog;
    }

    public static AlertDialog getRenamePlaylistDialog(View view, PlaylistWithSongsDB playlist, PlaylistsViewModel playlistsViewModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        final EditText edittext = new EditText(view.getContext());
        builder.setView(edittext);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String newName = edittext.getText().toString();
                playlistsViewModel.renamePlaylist(playlist, newName);
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });
        builder.setMessage("Enter new Name");
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
