package com.pictoslide.www.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;
import com.pictoslide.www.R;
import com.pictoslide.www.adapters.AlbumTabsAdapter;
import com.pictoslide.www.utils.MainUtil;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import butterknife.ButterKnife;

public class AlbumActivity extends AppCompatActivity {
    private MainUtil mMainUtil;
    private String mAlbumName;
    private String mAlbumDescription;
    private int mAlbumCategory;

    public static final String ALBUM_NAME_EXTRA = "album_name_extra";
    public static final String ALBUM_ID_EXTRA = "album_id_extra";
    public static final String ALBUM_DESCRIPTION_EXTRA = "album_description_extra";
    public static final String ALBUM_CATEGORY_EXTRA = "album_category_extra";
    public static final String ALBUM_INFO_ALL_SET_EXTRA = "album_info_all_set_extra";
    public static final String PHOTO_ITEMS_LIST_PREFS = "photo_items_list_prefs";
    public static final String MUSIC_DATA_LIST_PREFS = "music_data_list_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        ButterKnife.bind(this);

        mMainUtil = new MainUtil(this);

        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));

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

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);
        ViewPager viewPager = findViewById(R.id.view_pager);

        // Create an adapter that knows which fragment should be shown on each page
        AlbumTabsAdapter albumTabsAdapter = new AlbumTabsAdapter(getSupportFragmentManager(), AlbumActivity.this);

        // Set the adapter onto the view pager
        viewPager.setAdapter(albumTabsAdapter);
        // Give the TabLayout the ViewPager
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(MainActivity.IS_NEW_ALBUM_EXTRA)) {
                boolean is_new_album_being_created = intent.getBooleanExtra(MainActivity.IS_NEW_ALBUM_EXTRA,false);
                if (is_new_album_being_created) {
                    getSupportActionBar().setTitle(R.string.new_album_label_text);
                } else {
                    getSupportActionBar().setTitle(R.string.edit_album_label_text);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.preview_album) {
            // preview album
            boolean isAllInfoForAlbumSavedSuccessfully = mMainUtil.getDataBooleanFromSharedPreferences(ALBUM_INFO_ALL_SET_EXTRA);
            // check if all album info data has been saved successfully
            if (!isAllInfoForAlbumSavedSuccessfully) {
                mMainUtil.showToastMessage(getString(R.string.specify_album_info));
                return false;
            }
            // retrieve all album info data from preference
            retrieveAllInfoInfoFromPrefs();
            // check if one or more photos have been added to library
            Set<String> photoAbsolutePathsSet = mMainUtil.getDataArrayListStringFromSharedPreferences(PHOTO_ITEMS_LIST_PREFS);
            if (photoAbsolutePathsSet == null) {
                mMainUtil.showToastMessage(getString(R.string.at_least_1_photo));
                return false;
            } else {
                if (photoAbsolutePathsSet.isEmpty()) {
                    mMainUtil.showToastMessage(getString(R.string.at_least_1_photo));
                    return false;
                }
            }
            ArrayList<String> photoAbsolutePathsList = new ArrayList<>(photoAbsolutePathsSet);
            // check if one or more music have been added to the album
            Set<String> musicIdsDataSet = mMainUtil.getDataArrayListStringFromSharedPreferences(MUSIC_DATA_LIST_PREFS);
            if (musicIdsDataSet == null) {
                mMainUtil.showToastMessage(getString(R.string.at_least_1_music));
                return false;
            } else {
                if (musicIdsDataSet.isEmpty()) {
                    mMainUtil.showToastMessage(getString(R.string.at_least_1_music));
                    return false;
                }
            }
            ArrayList<String> musicIdsDataList = new ArrayList<>(musicIdsDataSet);
            // go to AlbumViewer activity
            Intent intent = new Intent(this,AlbumViewerActivity.class);
            // pass album data info
            intent.putExtra(ALBUM_NAME_EXTRA,mAlbumName);
            intent.putExtra(ALBUM_DESCRIPTION_EXTRA,mAlbumDescription);
            intent.putExtra(ALBUM_CATEGORY_EXTRA,mAlbumCategory);
            // pass photo absolute paths data
            intent.putExtra(PHOTO_ITEMS_LIST_PREFS, photoAbsolutePathsList);
            // pass music ids data
            intent.putExtra(MUSIC_DATA_LIST_PREFS, musicIdsDataList);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void retrieveAllInfoInfoFromPrefs() {
        mAlbumName = mMainUtil.getDataStringFromSharedPreferences(ALBUM_NAME_EXTRA);
        mAlbumDescription = mMainUtil.getDataStringFromSharedPreferences(ALBUM_DESCRIPTION_EXTRA);
        mAlbumCategory = mMainUtil.getDataIntFromSharedPreferences(ALBUM_CATEGORY_EXTRA);
    }
}