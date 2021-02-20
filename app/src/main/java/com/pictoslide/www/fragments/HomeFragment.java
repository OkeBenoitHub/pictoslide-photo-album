package com.pictoslide.www.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.pictoslide.www.R;
import com.pictoslide.www.activities.AlbumActivity;
import com.pictoslide.www.utils.MainUtil;

import static com.pictoslide.www.activities.MainActivity.IS_NEW_ALBUM_EXTRA;
import static com.pictoslide.www.activities.MainActivity.MADE_CHANGES_ALBUM_LIBRARY;

public class HomeFragment extends Fragment {
    private MainUtil mMainUtil;
    private boolean isTabletDevice;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainUtil = new MainUtil(requireContext());
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView;
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if (rootView.findViewById(R.id.album_library_fragment_tablet) != null) {
            LinearLayout album_library_fragment_tablet = rootView.findViewById(R.id.album_library_fragment_tablet);
            isTabletDevice = album_library_fragment_tablet.getVisibility() == View.VISIBLE;
        }

        boolean hasMadeChangesFromAlbumLibrary = mMainUtil.getDataBooleanFromSharedPreferences(MADE_CHANGES_ALBUM_LIBRARY);
        if (hasMadeChangesFromAlbumLibrary && !isTabletDevice) {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_HomeFragment_to_AlbumsFragment);
            mMainUtil.clearPreferenceDataByKey(MADE_CHANGES_ALBUM_LIBRARY);
        }

        // share app button tapped
        rootView.findViewById(R.id.shareAppButton).setOnClickListener(view1 -> {
            if (rootView.findViewById(R.id.shareAppButton).isEnabled()) {
                String aboutAppText = getString(R.string.about_app_text);
                mMainUtil.shareTextData(getString(R.string.share_app_via_text),aboutAppText);
            }
        });

        // create new album button tapped
        rootView.findViewById(R.id.newAlbumButton).setOnClickListener(view1 -> {
            Intent intent = new Intent(getContext(), AlbumActivity.class);
            intent.putExtra(IS_NEW_ALBUM_EXTRA, true);
            startActivity(intent);
        });

        int nightModeFlags =
                requireContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;

        boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        if (isNightMode) {
            // change background logo black in dark mode
            ImageView mainLogoImageView = rootView.findViewById(R.id.main_logo);
            mainLogoImageView.setImageResource(R.drawable.pictoslide_128_black);
        }

        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!isTabletDevice) {
            inflater.inflate(R.menu.library_item_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_album_library) {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_HomeFragment_to_AlbumsFragment);
        }
        return super.onOptionsItemSelected(item);
    }
}