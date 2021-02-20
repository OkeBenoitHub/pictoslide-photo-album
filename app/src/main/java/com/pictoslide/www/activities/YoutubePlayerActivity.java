package com.pictoslide.www.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.pictoslide.www.R;
import com.pictoslide.www.fragments.AlbumMusicFragment;
import com.pictoslide.www.utils.MainUtil;

public class YoutubePlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    public static String API_KEY;
    public static final String MUSIC_ITEM_POSITION_EXTRA = "music_item_position_extra";
    public static final String MUSIC_ITEM_ID_EXTRA = "music_item_id_extra";
    public static final String MUSIC_ITEM_PERFORM_ACTION_EXTRA = "music_item_perform_action_extra";
    private String music_intent_id;
    private int music_intent_position;
    private String music_intent_perform_action;
    private MainUtil mMainUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        mMainUtil = new MainUtil(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        int nightModeFlags =
                getApplicationContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;

        boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        if (isNightMode && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        API_KEY = getString(R.string.youtube_api_key_pictoslide);

        Button addOrRemoveMusicItemFromAlbum = findViewById(R.id.addRemoveMusicFromAlbum);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(MUSIC_ITEM_ID_EXTRA)) {
                music_intent_id = intent.getStringExtra(MUSIC_ITEM_ID_EXTRA);
            }
            if (intent.hasExtra(MUSIC_ITEM_POSITION_EXTRA)) {
                music_intent_position = intent.getIntExtra(MUSIC_ITEM_POSITION_EXTRA,-1);
            }
            if (intent.hasExtra(MUSIC_ITEM_PERFORM_ACTION_EXTRA)) {
                music_intent_perform_action = intent.getStringExtra(MUSIC_ITEM_PERFORM_ACTION_EXTRA);
                if (music_intent_perform_action.equalsIgnoreCase(AlbumMusicFragment.MUSIC_ITEM_ADD_TO_ALBUM_ACTION)) {
                    addOrRemoveMusicItemFromAlbum.setText(R.string.Add_music_to_album_btn);
                } else if (music_intent_perform_action.equalsIgnoreCase(AlbumMusicFragment.MUSIC_ITEM_REMOVE_FROM_ALBUM_ACTION)) {
                    addOrRemoveMusicItemFromAlbum.setText(R.string.remove_music_from_album_btn);
                }
            }
        }

        // Initializing YouTube player view
        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);
        youTubePlayerView.initialize(API_KEY, this);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        if(youTubePlayer == null) return;
        // Start buffering
        if (!wasRestored) {
            if (music_intent_id.length() == 11) {
                youTubePlayer.loadVideo(music_intent_id);
            }
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        mMainUtil.showToastMessage(getString(R.string.failed_to_play_video_text));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void saveMusicToAlbum(View view) {
        Intent returnIntent = new Intent();
        if (music_intent_id.length() == 11 && music_intent_position != -1) {
            returnIntent.putExtra(MUSIC_ITEM_POSITION_EXTRA,music_intent_position);
            returnIntent.putExtra(MUSIC_ITEM_PERFORM_ACTION_EXTRA,music_intent_perform_action);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
    }
}