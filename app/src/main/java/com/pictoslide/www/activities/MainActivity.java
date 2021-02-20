package com.pictoslide.www.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pictoslide.www.R;
import com.pictoslide.www.models.Album;
import com.pictoslide.www.utils.AppConstUtils;
import com.pictoslide.www.utils.MainUtil;
import com.pictoslide.www.utils.NetworkUtil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import butterknife.ButterKnife;

import static com.pictoslide.www.activities.AlbumActivity.ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumActivity.MUSIC_DATA_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumActivity.PHOTO_ITEMS_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumViewerActivity.ALBUM_SUB_OWNERS_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumViewerActivity.OWNER_ALBUM_ID_EXTRA;

public class MainActivity extends AppCompatActivity {
    private MainUtil mMainUtil;
    private NetworkUtil mNetworkUtil;
    public static final String IS_NEW_ALBUM_EXTRA = "is_new_album_extra";
    public static final String MADE_CHANGES_ALBUM_LIBRARY = "made_changes_album_library";
    private FirebaseFirestore mFirebaseFirestore;
    private ProgressDialog mLoaderDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMainUtil = new MainUtil(this);
        mNetworkUtil = new NetworkUtil(this);
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        mMainUtil.getUniqueID(this); // generate an user id from device
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

        // add support for up button for fragment navigation
        //val navController = this.findNavController(R.id.myNavHostFragment);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(toolbar,navController);

        Intent intent = getIntent();
        if (intent != null) {
            FirebaseDynamicLinks.getInstance()
                    .getDynamicLink(intent)
                    .addOnSuccessListener(this, pendingDynamicLinkData -> {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }

                        if (deepLink != null) {
                            String dynamicAlbumLink = deepLink.toString();
                            // retrieve album id from link
                            String albumIntentId = dynamicAlbumLink.substring(38);
                            if (albumIntentId.length() == 20) {
                                // go to viewer activity based on album data from firestore
                                goToAlbumViewerActivityWithData(albumIntentId);
                            }
                        }
                    }).addOnFailureListener(this, e -> {
                        //Log.w(TAG, "getDynamicLink:onFailure", e);
                        mMainUtil.showToastMessage(getString(R.string.failed_retrieve_album_data));
                    });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp();
    }

    private void goToAlbumViewerActivityWithData(String albumIntentId) {
        // make sure we have some kind of internet
        if (!mNetworkUtil.isUserConnectedToNetwork()) {
            mMainUtil.showToastMessage(getString(R.string.no_internet_connection));
        }
        // create progress dialog
        mLoaderDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        mLoaderDialog.setMessage(getString(R.string.retrieving_album_data_text));
        mLoaderDialog.setCancelable(false);
        mLoaderDialog.setCanceledOnTouchOutside(false);
        mLoaderDialog.show();
        DocumentReference docRef = mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(albumIntentId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // album does not exist anymore
                    //Log.d(TAG, "No such document");
                    mLoaderDialog.dismiss();
                    mMainUtil.showToastMessage(getString(R.string.owner_deleted_album_text));
                } else {
                    // album still exists
                    Album album = document.toObject(Album.class);
                    String currentUserId = mMainUtil.getUniqueID(getApplicationContext());
                    if (album != null) {
                        Intent intent = new Intent(getApplicationContext(),AlbumViewerActivity.class);
                        intent.putExtra(AlbumActivity.ALBUM_NAME_EXTRA,album.getName());
                        intent.putExtra(AlbumActivity.ALBUM_CATEGORY_EXTRA,album.getCategory());
                        intent.putExtra(AlbumActivity.ALBUM_DESCRIPTION_EXTRA,album.getDescription());
                        ArrayList<String> photoAbsolutePathsListData = (ArrayList<String>) album.getPhotoPathsList();
                        // pass photo data
                        intent.putExtra(PHOTO_ITEMS_LIST_PREFS,photoAbsolutePathsListData);
                        ArrayList<String> musicIdsDataListData = (ArrayList<String>) album.getMusicIdsList();
                        // pass music data data
                        intent.putExtra(MUSIC_DATA_LIST_PREFS,musicIdsDataListData);
                        ArrayList<String> albumSubOwnersId = (ArrayList<String>) album.getSubOwnersId();
                        // pass sub owners id data
                        intent.putExtra(ALBUM_SUB_OWNERS_ID_EXTRA,albumSubOwnersId);
                        intent.putExtra(IS_NEW_ALBUM_EXTRA, albumSubOwnersId.contains(currentUserId) || currentUserId.equals(album.getOwnerId()));
                        intent.putExtra(OWNER_ALBUM_ID_EXTRA,album.getOwnerId());
                        intent.putExtra(ALBUM_ID_EXTRA,album.getAlbumId());
                        mLoaderDialog.dismiss();
                        startActivity(intent);
                    }

                }
            } else {
                mLoaderDialog.dismiss();
                mMainUtil.showToastMessage(getString(R.string.owner_deleted_album_text));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMainUtil.clearPreferenceDataByKey(AlbumActivity.ALBUM_INFO_ALL_SET_EXTRA);
        mMainUtil.clearPreferenceDataByKey(AlbumActivity.PHOTO_ITEMS_LIST_PREFS);
        mMainUtil.clearPreferenceDataByKey(AlbumActivity.MUSIC_DATA_LIST_PREFS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        String[] subjects = {"oke.benoit@gmail.com"};
        if (id == R.id.action_feedback) {
            mMainUtil.composeEmail(subjects, getString(R.string.feedback_menu_text), getString(R.string.give_us_feedback_text), getString(R.string.send_feedback_via_text));
        } else if (id == R.id.action_report_issue) {
            mMainUtil.composeEmail(subjects, getString(R.string.report_an_issue_text), getString(R.string.what_went_wrong_text), getString(R.string.send_report_via_text));
        } else if (id == R.id.action_share_app) {
            String aboutAppText = getString(R.string.about_app_text);
            aboutAppText += "\n";
            aboutAppText += getString(R.string.about_album_share_3) + "\n";
            aboutAppText += getString(R.string.app_play_store_link);
            mMainUtil.shareTextData(getString(R.string.share_app_via_text), aboutAppText);
        }
        return super.onOptionsItemSelected(item);
    }
}