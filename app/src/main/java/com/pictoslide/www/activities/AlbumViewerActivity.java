package com.pictoslide.www.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.glide.slider.library.SliderLayout;
import com.glide.slider.library.animations.DescriptionAnimation;
import com.glide.slider.library.slidertypes.BaseSliderView;
import com.glide.slider.library.slidertypes.TextSliderView;
import com.glide.slider.library.tricks.ViewPagerEx;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pictoslide.www.BuildConfig;
import com.pictoslide.www.R;
import com.pictoslide.www.models.Album;
import com.pictoslide.www.utils.AppConstUtils;
import com.pictoslide.www.utils.MainUtil;
import com.pictoslide.www.utils.NetworkUtil;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.pictoslide.www.activities.AlbumActivity.ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.MainActivity.IS_NEW_ALBUM_EXTRA;
import static com.pictoslide.www.activities.MainActivity.MADE_CHANGES_ALBUM_LIBRARY;

public class AlbumViewerActivity extends AppCompatActivity implements BaseSliderView.OnSliderClickListener,
        ViewPagerEx.OnPageChangeListener {
    private MainUtil mMainUtil;
    private NetworkUtil mNetworkUtil;
    private FirebaseFirestore mFirebaseFirestore;

    private SliderLayout mPhotoSlider;
    YouTubePlayerView mYouTubePlayerView;

    private String mAlbumName;
    private String mAlbumDescription;
    private int mAlbumCategory;

    private ArrayList<String> mPhotoAbsolutePathsList;
    private ArrayList<String> mMusicIdsDataList;
    private int music_play_index;
    List<String> musicVideoIds;
    List<String> photoPathsList;
    List<String> photoCaptionsList;
    List<String> musicTitlesData;
    private String mPlayerState;
    private YouTubePlayer mYoutubePlayer;
    private boolean mIsAllMusicFinishedPlaying;
    private boolean mIsExistingAlbum;
    private String mCurrentUserId;
    private String mAlbumOwnerId;
    private String mAlbumIntentId;
    private List<String> mAlbumSubOwnersId;
    public static final String OWNER_ALBUM_ID_EXTRA = "owner_album_id_extra";
    public static final String ALBUM_SUB_OWNERS_ID_EXTRA = "album_sub_owners_id_extra";

    private LinearLayout mBottomViewerLayout;
    private TextView holderTextV;
    private TextView errorPlayingMusicTV;
    private ImageView holderThumb;

    private StorageReference mStorageReference;
    private ProgressDialog mLoaderDialog;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_viewer);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.bottom_black_color));
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.bottom_black_color));
        }

        mMainUtil = new MainUtil(this);
        mNetworkUtil = new NetworkUtil(this);
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = firebaseStorage.getReference();

        mPhotoSlider = findViewById(R.id.slider);

        mBottomViewerLayout = findViewById(R.id.bottom_box_viewer);
        mBottomViewerLayout.setVisibility(View.INVISIBLE);

        mCurrentUserId = mMainUtil.getUniqueID(this);

        holderThumb = findViewById(R.id.music_thumbnail);
        holderTextV = findViewById(R.id.music_title);
        holderTextV.setSelected(true);
        errorPlayingMusicTV = findViewById(R.id.error_playing_music);

        if (!mNetworkUtil.isUserConnectedToNetwork()) {
            mMainUtil.showToastMessage(getString(R.string.No_internet_available_text));
            errorPlayingMusicTV.setVisibility(View.VISIBLE);
        } else {
            showBufferingMusic();
        }

        // create progress dialog
        mLoaderDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);

        Intent intent = getIntent();
        if (intent != null) {
            int i = 0;
            if (intent.hasExtra(AlbumActivity.ALBUM_NAME_EXTRA)) {
                mAlbumName = intent.getStringExtra(AlbumActivity.ALBUM_NAME_EXTRA);
                i += 1;
            }
            if (intent.hasExtra(AlbumActivity.ALBUM_DESCRIPTION_EXTRA)) {
                mAlbumDescription = intent.getStringExtra(AlbumActivity.ALBUM_DESCRIPTION_EXTRA);
                TextView albumDescription = findViewById(R.id.about_album_description);
                albumDescription.setText(mAlbumDescription);
                i += 1;
            }
            if (intent.hasExtra(AlbumActivity.ALBUM_CATEGORY_EXTRA)) {
                mAlbumCategory = intent.getIntExtra(AlbumActivity.ALBUM_CATEGORY_EXTRA, -1);
                i += 1;
            }

            if (intent.hasExtra(AlbumActivity.PHOTO_ITEMS_LIST_PREFS)) {
                mPhotoAbsolutePathsList = intent.getStringArrayListExtra(AlbumActivity.PHOTO_ITEMS_LIST_PREFS);
                i += 1;
            }
            if (intent.hasExtra(AlbumActivity.MUSIC_DATA_LIST_PREFS)) {
                mMusicIdsDataList = intent.getStringArrayListExtra(AlbumActivity.MUSIC_DATA_LIST_PREFS);
                i += 1;
            }
            if (intent.hasExtra(IS_NEW_ALBUM_EXTRA)) {
                mIsExistingAlbum = intent.getBooleanExtra(IS_NEW_ALBUM_EXTRA,false);
            }

            mAlbumOwnerId = mCurrentUserId;
            if (intent.hasExtra(OWNER_ALBUM_ID_EXTRA)) {
                mAlbumOwnerId = intent.getStringExtra(OWNER_ALBUM_ID_EXTRA);
            }
            mAlbumIntentId = null;
            if (intent.hasExtra(ALBUM_ID_EXTRA)) {
                mAlbumIntentId = intent.getStringExtra(ALBUM_ID_EXTRA);
            }
            mAlbumSubOwnersId = new ArrayList<>();
            if (intent.hasExtra(ALBUM_SUB_OWNERS_ID_EXTRA)) {
                mAlbumSubOwnersId = intent.getStringArrayListExtra(ALBUM_SUB_OWNERS_ID_EXTRA);
                checkIfAlbumStillAvailable(mAlbumIntentId,true);
            }

            if (i == 5) {
                RequestOptions requestOptions = new RequestOptions();
                requestOptions.fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(new ColorDrawable(Color.GRAY))
                        .error(R.drawable.pictoslide_256);

                photoPathsList = new ArrayList<>();
                photoCaptionsList = new ArrayList<>();
                for (int j = 0; j < mPhotoAbsolutePathsList.size(); j++) {
                    TextSliderView sliderView = new TextSliderView(this);
                    String[] photoData = mPhotoAbsolutePathsList.get(j).split("\\|", -1);
                    photoPathsList.add(photoData[0]);
                    photoCaptionsList.add(photoData[1]);
                    // initialize SliderLayout
                    if (photoData[0].contains("https")) {
                        // slider from https images fro url
                        sliderView
                                .image(photoData[0])
                                .description(photoData[1])
                                .setRequestOption(requestOptions)
                                .setProgressBarVisible(true)
                                .setOnSliderClickListener(this);
                    } else {
                        // slider from file image
                        sliderView
                                .image(new File(photoData[0]))
                                .description(photoData[1])
                                .setRequestOption(requestOptions)
                                .setProgressBarVisible(true)
                                .setOnSliderClickListener(this);
                    }

                    mPhotoSlider.addSlider(sliderView);
                }

                // set Slider Transition Animation
                setRandomPresetTransformersToSlider();
                mPhotoSlider.setDuration(8000);
                mPhotoSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Top);
                mPhotoSlider.setCustomAnimation(new DescriptionAnimation());
                mPhotoSlider.addOnPageChangeListener(this);
                mPhotoSlider.stopCyclingWhenTouch(false);

                // Initializing YouTube player view
                mYouTubePlayerView = findViewById(R.id.player_view);
                getLifecycle().addObserver(mYouTubePlayerView);
                mYouTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                        mYoutubePlayer = youTubePlayer;
                        errorPlayingMusicTV.setText(getString(R.string.buffering_music_text));
                        errorPlayingMusicTV.setVisibility(View.VISIBLE);
                        loadFirstMusicById();
                    }

                    @Override
                    public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                        super.onStateChange(youTubePlayer, state);
                        mYoutubePlayer = youTubePlayer;
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            loadNextAvailableVideo();
                        }
                        if (state == PlayerConstants.PlayerState.BUFFERING) {
                            mPlayerState = "BUFFERING";
                            //mBottomViewerLayout.setVisibility(View.INVISIBLE);
                            errorPlayingMusicTV.setText(getString(R.string.buffering_music_text));
                            errorPlayingMusicTV.setVisibility(View.VISIBLE);
                        }
                        if (state == PlayerConstants.PlayerState.PAUSED) {
                            mPlayerState = "PAUSED";
                        }
                        if (state == PlayerConstants.PlayerState.PLAYING) {
                            mPlayerState = "PLAYING";
                            holderTextV.setSelected(true);
                            mBottomViewerLayout.setVisibility(View.VISIBLE);
                            hideBufferingMusic();
                        }
                        if (!mNetworkUtil.isUserConnectedToNetwork()) {
                            errorPlayingMusicTV.setText(getString(R.string.pictoslide_needs_internet_to_play_music));
                            errorPlayingMusicTV.setVisibility(View.VISIBLE);
                        } else {
                            errorPlayingMusicTV.setText(getString(R.string.pictoslide_needs_internet_to_play_music));
                            errorPlayingMusicTV.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                        super.onError(youTubePlayer, error);
                        mYoutubePlayer = youTubePlayer;
                        loadNextAvailableVideo();
                    }
                });
            }
        }

        MaterialRippleLayout addMusicToDb = findViewById(R.id.addMusicToDb);
        MaterialRippleLayout removeMusicFromDb = findViewById(R.id.removeMusicFromDb);
        if (mIsExistingAlbum) {
            addMusicToDb.setVisibility(View.GONE);
            removeMusicFromDb.setVisibility(View.VISIBLE);
            removeMusicFromDb.setOnClickListener(view -> saveAlbumToDatabase(false));
        } else {
            removeMusicFromDb.setVisibility(View.GONE);
            addMusicToDb.setVisibility(View.VISIBLE);
            addMusicToDb.setOnClickListener(view -> saveAlbumToDatabase(true));
        }
        // album description button tapped
        MaterialRippleLayout albumDescriptionBtn = findViewById(R.id.albumDescriptionButton);
        LinearLayout about_album_description_box = findViewById(R.id.about_album_description_box);
        albumDescriptionBtn.setOnClickListener(view -> about_album_description_box.setVisibility(View.VISIBLE));
        MaterialRippleLayout closeAlbumDescriptionBtn = findViewById(R.id.closeAlbumDescriptionButton);
        closeAlbumDescriptionBtn.setOnClickListener(view -> about_album_description_box.setVisibility(View.INVISIBLE));

        // skip next video button from album tapped
        MaterialRippleLayout skipNextVideoFromAlbum = findViewById(R.id.skip_video_album_playing);
        skipNextVideoFromAlbum.setOnClickListener(view -> {
            setRandomPresetTransformersToSlider();
            loadNextAvailableVideo();
        });
    }

    private void showBufferingMusic() {
        errorPlayingMusicTV.setText(getString(R.string.loading_music_text));
        errorPlayingMusicTV.setVisibility(View.VISIBLE);
    }
    private void hideBufferingMusic() {
        errorPlayingMusicTV.setText(getString(R.string.pictoslide_needs_internet_to_play_music));
        errorPlayingMusicTV.setVisibility(View.INVISIBLE);
    }

    private void loadNextAvailableVideo() {
        if (mNetworkUtil.isUserConnectedToNetwork()) {
            music_play_index = music_play_index + 1;
            if (music_play_index >= musicVideoIds.size()) {
                if (musicVideoIds.size() >= 1) {
                    Random random = new Random();
                    music_play_index = random.nextInt(musicVideoIds.size());
                    showBufferingMusic();
                    mBottomViewerLayout.setVisibility(View.INVISIBLE);
                }
            }
            mYoutubePlayer.loadVideo(musicVideoIds.get(music_play_index), 0);
            loadMusicExtraData();
        } else {
            mMainUtil.showToastMessage(getString(R.string.No_internet_available_text));
            errorPlayingMusicTV.setVisibility(View.VISIBLE);
        }
    }

    private void shareAlbumPlaying(Album album) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://www.pictoslideapp.com/albumId/" + album.getAlbumId()))
                .setDomainUriPrefix("https://pictoslideapp.page.link")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).setMinimumVersion(1).build())
                // Set parameters
                // ...
                .buildShortDynamicLink()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Short link created
                        Uri shortLink = task.getResult().getShortLink();
                        if (shortLink != null) {
                            String album_share_text = getString(R.string.hey_text) + "\n";
                            album_share_text += getString(R.string.about_app_text) + "\n";
                            album_share_text += getString(R.string.about_album_share_1) + "\n";
                            album_share_text += shortLink.toString() + "\n";
                            album_share_text += getString(R.string.about_album_share_2) + "\n";
                            album_share_text += getString(R.string.about_album_share_3) + "\n";
                            album_share_text += getString(R.string.app_play_store_link);
                            mMainUtil.shareTextData(getString(R.string.share_album_via_title_text),album_share_text);
                        }
                    } else {
                        // Error
                        mMainUtil.showToastMessage(getString(R.string.No_internet_available_text));
                    }
                });
    }

    private void loadFirstMusicById() {
        if (mMusicIdsDataList != null) {
            if (mMusicIdsDataList.size() > 0) {
                musicVideoIds = new ArrayList<>();
                musicTitlesData = new ArrayList<>();
                for (int k = 0; k < mMusicIdsDataList.size(); k++) {
                    String[] musicData = mMusicIdsDataList.get(k).split("\\|", -1);
                    String musicIdData = musicData[0];
                    String titleData = musicData[1];
                    musicVideoIds.add(musicIdData);
                    musicTitlesData.add(titleData);
                }
                if (!musicVideoIds.isEmpty()) {
                    if (mNetworkUtil.isUserConnectedToNetwork()) {
                        music_play_index = 0;
                        mYoutubePlayer.loadVideo(musicVideoIds.get(music_play_index), 0);
                        loadMusicExtraData();
                    } else {
                        mMainUtil.showToastMessage(getString(R.string.No_internet_available_text));
                        errorPlayingMusicTV.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private void loadMusicExtraData() {
        holderTextV.setSelected(true);
        holderTextV.setText(musicTitlesData.get(music_play_index));
        String musicThumbUrl = "https://img.youtube.com/vi/" + musicVideoIds.get(music_play_index) + "/default.jpg";
        Glide.with(getApplicationContext())
                .load(musicThumbUrl)
                .fitCenter()
                .override(Target.SIZE_ORIGINAL, holderThumb.getHeight())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(new ColorDrawable(Color.GRAY))
                .into(holderThumb);
        if (mPhotoSlider != null) {
            mPhotoSlider.startAutoCycle();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mYouTubePlayerView != null) {
            mIsAllMusicFinishedPlaying = true;
            mYouTubePlayerView.release();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // user is logged in
        if (mPhotoSlider != null) {
            mPhotoSlider.startAutoCycle();
        }
        if (mYouTubePlayerView != null && mPlayerState != null) {
            if (mPlayerState.equals("PAUSED")) {
                if (mYoutubePlayer != null) {
                    if (mIsAllMusicFinishedPlaying) {
                        loadNextAvailableVideo();
                    }
                    mYoutubePlayer.play();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRandomPresetTransformersToSlider();
    }

    private void setRandomPresetTransformersToSlider() {
        if (mPhotoSlider != null) {
            SliderLayout.Transformer[] transformers = {
                    SliderLayout.Transformer.ZoomIn,
                    SliderLayout.Transformer.ZoomOut,
                    SliderLayout.Transformer.CubeIn,
                    SliderLayout.Transformer.Accordion,
                    SliderLayout.Transformer.Background2Foreground,
                    SliderLayout.Transformer.DepthPage,
                    SliderLayout.Transformer.FlipHorizontal,
                    SliderLayout.Transformer.FlipPage,
                    SliderLayout.Transformer.Default,
                    SliderLayout.Transformer.RotateDown,
                    SliderLayout.Transformer.RotateUp,
                    SliderLayout.Transformer.Stack,
                    SliderLayout.Transformer.Tablet,
                    SliderLayout.Transformer.ZoomOutSlide,
            };
            mPhotoSlider.setPresetTransformer(transformers[new Random().nextInt(transformers.length)]);
        }
    }

    @Override
    protected void onStop() {
        // To prevent a memory leak on rotation, make sure to call stopAutoCycle() on the slider before activity or fragment is destroyed
        if (mPhotoSlider != null) {
            mPhotoSlider.stopAutoCycle();
        }
        if (mYouTubePlayerView != null) {
            if (mYoutubePlayer != null && mIsAllMusicFinishedPlaying) {
                mYouTubePlayerView.release();
            }
        }
        super.onStop();
    }

    public void saveAlbumToDatabase(boolean shouldAddAlbumToLibrary) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(AlbumViewerActivity.this,R.style.Theme_MaterialComponents_DayNight_Dialog_Alert);

        // Set a title for alert dialog
        builder.setTitle(R.string.save_album_title_text);

        // Ask the final question
        if (shouldAddAlbumToLibrary) {
            builder.setMessage(R.string.save_album_confirm_text);
        } else {
            builder.setMessage(R.string.remove_album_from_library_text);
        }

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            if (mNetworkUtil.isUserConnectedToNetwork()) {
                if (shouldAddAlbumToLibrary) {
                    mLoaderDialog.setMessage(getString(R.string.saving_album_to_library));
                } else {
                    mLoaderDialog.setMessage(getString(R.string.removing_album_from_library));
                }
                mLoaderDialog.setCancelable(false);
                mLoaderDialog.setCanceledOnTouchOutside(false);
                mLoaderDialog.show();
                if (shouldAddAlbumToLibrary) {
                    if (mAlbumSubOwnersId != null && !mCurrentUserId.equals(mAlbumOwnerId))
                        mAlbumSubOwnersId.add(mCurrentUserId);
                    addAlbumToLibrary();
                } else {
                    if (mAlbumSubOwnersId != null && !mCurrentUserId.equals(mAlbumOwnerId))
                        mAlbumSubOwnersId.remove(mCurrentUserId);
                    removeAlbumFromLibrary();
                }
            } else {
                mMainUtil.showToastMessage(getString(R.string.no_internet_connection));
                errorPlayingMusicTV.setVisibility(View.VISIBLE);
            }
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    private void checkIfAlbumStillAvailable(String albumIntentId, boolean shouldInitCheck) {
        // make sure we have some kind of internet
        if (!mNetworkUtil.isUserConnectedToNetwork()) {
            if (mLoaderDialog.isShowing())
                mLoaderDialog.dismiss();
            mMainUtil.showToastMessage(getString(R.string.no_internet_connection));
            return;
        }
        DocumentReference docRef = mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(albumIntentId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // album does not exist anymore
                    //Log.d(TAG, "No such document");
                    mMainUtil.showToastMessage(getString(R.string.owner_deleted_album_text));
                    goBackToMainActivity(false);
                } else {
                    // album still exists
                    Album albumItem = document.toObject(Album.class);
                    if (albumItem != null) {
                        mAlbumSubOwnersId = albumItem.getSubOwnersId();
                        if (mIsExistingAlbum) {
                            // remove
                            mAlbumSubOwnersId.remove(mCurrentUserId);
                        } else {
                            // add
                            mAlbumSubOwnersId.add(mCurrentUserId);
                        }
                        // share album playing
                        MaterialRippleLayout share_album_playing = findViewById(R.id.share_album_playing);
                        share_album_playing.setVisibility(View.VISIBLE);
                        share_album_playing.setOnClickListener(view -> shareAlbumPlaying(albumItem));
                    }
                    if (shouldInitCheck) {
                        return;
                    }
                    if (mAlbumSubOwnersId != null && mAlbumIntentId != null) {
                        DocumentReference albumOwnerRef = mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(mAlbumIntentId);
                        albumOwnerRef
                                .update("subOwnersId", mAlbumSubOwnersId)
                                .addOnSuccessListener(aVoid -> {
                                    mLoaderDialog.dismiss();
                                    if (mIsExistingAlbum) {
                                        mMainUtil.showToastMessage(getString(R.string.removed_successfully));
                                    } else {
                                        mMainUtil.showToastMessage(getString(R.string.added_successfully));
                                    }
                                    goBackToMainActivity(true);
                                })
                                .addOnFailureListener(e -> {
                                    mLoaderDialog.dismiss();
                                    mMainUtil.showToastMessage(getString(R.string.failed_perform_operation));
                                });
                    }
                }
            } else {
                mLoaderDialog.dismiss();
                mMainUtil.showToastMessage(getString(R.string.owner_deleted_album_text));
                goBackToMainActivity(false);
            }
        });
    }

    private void removeAlbumFromLibrary() {
        if (!mCurrentUserId.equals(mAlbumOwnerId)) {
            // remove someone else album from my library
            checkIfAlbumStillAvailable(mAlbumIntentId,false);
        } else {
            // remove my own album from library
            if (mAlbumIntentId != null) {
                // make sure we have some kind of internet
                if (!mNetworkUtil.isUserConnectedToNetwork()) {
                    if (mLoaderDialog.isShowing())
                        mLoaderDialog.dismiss();
                    mMainUtil.showToastMessage(getString(R.string.no_internet_connection));
                    return;
                }
                // remove my own album from library
                mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(mAlbumIntentId)
                        .delete()
                        .addOnSuccessListener(aVoid -> deletePhotosFromAlbum(mAlbumIntentId))
                        .addOnFailureListener(e -> {
                            //Log.w(TAG, "Error deleting document", e);
                            mMainUtil.showToastMessage(getString(R.string.failed_remove_album_library));
                        });
            }
        }
    }

    private void deletePhotosFromAlbum(String albumId) {
        if (photoPathsList.size() > 0) {
            for (int i = 0; i < photoPathsList.size(); i++) {
                String photoFullPathFromAlbum = photoPathsList.get(i);
                String photoRefNameFromAlbum = photoFullPathFromAlbum.substring(photoFullPathFromAlbum.indexOf("JPEG"),photoFullPathFromAlbum.indexOf(".jpg")) + ".jpg";
                StorageReference photoFromAlbumRef = mStorageReference.child(mCurrentUserId + "/albums/" + albumId + "/" + photoRefNameFromAlbum);
                int finalI1 = i;
                photoFromAlbumRef.delete().addOnSuccessListener(aVoid -> {
                    // File deleted successfully
                    if (finalI1 == photoPathsList.size() - 1) {
                        mLoaderDialog.dismiss();
                        mMainUtil.showToastMessage(getString(R.string.album_removed_from_library_text));
                        goBackToMainActivity(true);
                    }
                }).addOnFailureListener(exception -> {
                    // Uh-oh, an error occurred!
                    mLoaderDialog.dismiss();
                    mMainUtil.showToastMessage(getString(R.string.failed_delete_photos_from_album));
                });
            }
        }
    }

    private void addAlbumToLibrary() {
        if (mAlbumIntentId != null && !mCurrentUserId.equals(mAlbumOwnerId)) {
            // add someone else album to my library
            checkIfAlbumStillAvailable(mAlbumIntentId,false);
        } else {
            // add my own album to library
            String ownerId = mCurrentUserId;
            // create your own album
            String albumCreatedAtTime = new Date().getTime() + "";
            String albumCreatedAtDate = DateTimeUtils.formatWithPattern(new Date(), "EEEE, MMMM dd, yyyy");

            List<String> photoPathUrlsList = new ArrayList<>();
            List<String> musicDataList = new ArrayList<>();

            for (int j = 0; j < musicVideoIds.size(); j++) {
                musicDataList.add(musicVideoIds.get(j) + "|" + musicTitlesData.get(j));
            }
            // make sure there's som kind of internet connection
            if (!mNetworkUtil.isUserConnectedToNetwork()) {
                if (mLoaderDialog.isShowing())
                    mLoaderDialog.dismiss();
                mMainUtil.showToastMessage(getString(R.string.no_internet_connection));
                return;
            }
            // insert new album data to database
            Album newAlbumData = new Album("", ownerId, mAlbumName, mAlbumCategory, mAlbumDescription, "", photoPathUrlsList, musicDataList, mAlbumSubOwnersId, albumCreatedAtTime, albumCreatedAtDate);
            mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME)
                    .add(newAlbumData)
                    .addOnSuccessListener(documentReference -> {
                        //DocumentReference newAlbumRef = mFirebaseFirestore.collection("Albums").document(ownerId);
                        // loop through all photo files to upload
                        // Create file metadata including the content type
                        for (int i = 0; i < photoPathsList.size(); i++) {
                            String photoAbsolutePath = photoPathsList.get(i);
                            Uri photoFile = Uri.fromFile(new File(photoAbsolutePath));
                            StorageReference albumPhotosRef = mStorageReference.child(ownerId + "/albums/" + documentReference.getId() + "/" + photoFile.getLastPathSegment());
                            StorageMetadata metadata = new StorageMetadata.Builder()
                                    .setContentType("image/jpg")
                                    .build();
                            UploadTask uploadTask = albumPhotosRef.putFile(photoFile, metadata);

                            int finalI = i;
                            uploadTask.continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw Objects.requireNonNull(task.getException());
                                }

                                // Continue with the task to get the download URL
                                return albumPhotosRef.getDownloadUrl();
                            }).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Uri downloadUri = task.getResult();
                                    photoPathUrlsList.add(downloadUri.toString() + "|" + photoCaptionsList.get(finalI));
                                    documentReference.update("photoPathsList", photoPathUrlsList,
                                            "albumId", documentReference.getId())
                                            .addOnCompleteListener(task1 -> {
                                                if (finalI == photoPathUrlsList.size() - 1) {
                                                    mLoaderDialog.dismiss();
                                                    mMainUtil.showToastMessage(getString(R.string.added_to_library_text));
                                                    goBackToMainActivity(true);
                                                }
                                            });
                                }
                            });
                        }
                    });
        }
    }

    @Override
    public void onSliderClick(BaseSliderView slider) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void goBackToMainActivity(boolean hasMadeChangesFromAlbumLibrary) {
        Intent goToHomeActivityIntent = new Intent(AlbumViewerActivity.this, MainActivity.class);
        goToHomeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mMainUtil.writeDataBooleanToSharedPreferences(MADE_CHANGES_ALBUM_LIBRARY,hasMadeChangesFromAlbumLibrary);
        startActivity(goToHomeActivityIntent);
        finish();
    }
}