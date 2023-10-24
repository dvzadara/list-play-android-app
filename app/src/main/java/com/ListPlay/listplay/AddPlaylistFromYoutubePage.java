package com.ListPlay.listplay;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class AddPlaylistFromYoutubePage extends Fragment {
    EditText nameTextEdit;
    EditText linkTextEdit;
    Button savePlaylistButton;

    public AddPlaylistFromYoutubePage() {
        super(R.layout.fragment_add_playlist_from_youtube);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_add_playlist_from_youtube, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity mainActivity = ((MainActivity) requireActivity());
        savePlaylistButton = mainActivity.findViewById(R.id.savePlaylistButton);

        nameTextEdit = mainActivity.findViewById(R.id.editTextPlaylistName);
        linkTextEdit = mainActivity.findViewById(R.id.editTextYoutubeLink);

        savePlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadPlaylist(view);
            }
        });

    }

    public void downloadPlaylist(View view){
        String playlistName = nameTextEdit.getText().toString();
        String playlistYoutubeLink = linkTextEdit.getText().toString();

        MainActivity mainActivity = ((MainActivity) requireActivity());
        mainActivity.setPage(mainActivity.playlistsPage);
        mainActivity.addPlaylistFromYoutube(playlistName, playlistYoutubeLink);
    }
}