package com.ListPlay.listplay;

import android.content.DialogInterface;
import android.view.View;
import android.app.AlertDialog;
import android.widget.EditText;

import java.io.File;

public class AddSongToPlaylistDialog {
    public static AlertDialog getAddSongToPlaylistDialog(MainActivity mainActivity, PlaylistDB playlist, PlaylistsViewModel playlistsViewModel){
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        final EditText edittext = new EditText(mainActivity);
        builder.setView(edittext);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String songsUrl = edittext.getText().toString();
                File playlistDir = new File(mainActivity.getFilesDir(), playlist.getId() + "");
                Thread addPlaylistThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            YoutubeAudioDownloader.YoutubePlaylistToFiles(songsUrl, playlist.getId(), playlistDir, mainActivity, playlistsViewModel);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                addPlaylistThread.start();
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });
        builder.setMessage("Enter youtube link");
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
