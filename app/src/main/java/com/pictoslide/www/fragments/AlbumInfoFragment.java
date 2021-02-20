package com.pictoslide.www.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.pictoslide.www.R;
import com.pictoslide.www.utils.MainUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlbumInfoFragment extends Fragment {
    private MainUtil mMainUtil;
    private int mAlbumCategory;

    private EditText mAlbumNameEditText;
    private EditText mAlbumDescriptionEditText;
    private String mAlbumName;
    private String mAlbumDescription;
    private boolean isAllInfoForAlbumSavedSuccessfully;

    private static final String ALBUM_NAME_EXTRA = "album_name_extra";
    private static final String ALBUM_DESCRIPTION_EXTRA = "album_description_extra";
    private static final String ALBUM_CATEGORY_EXTRA = "album_category_extra";
    private static final String ALBUM_INFO_ALL_SET_EXTRA = "album_info_all_set_extra";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mMainUtil = new MainUtil(requireContext());
        isAllInfoForAlbumSavedSuccessfully = false;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ALBUM_INFO_ALL_SET_EXTRA)) {
                isAllInfoForAlbumSavedSuccessfully = savedInstanceState.getBoolean(ALBUM_INFO_ALL_SET_EXTRA);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // if all album info data has been saved successfully :: cached them in case of screen rotation
        if (isAllInfoForAlbumSavedSuccessfully) {
            outState.putString(ALBUM_NAME_EXTRA,mAlbumName);
            outState.putString(ALBUM_DESCRIPTION_EXTRA,mAlbumDescription);
            outState.putInt(ALBUM_CATEGORY_EXTRA,mAlbumCategory);
            outState.putBoolean(ALBUM_INFO_ALL_SET_EXTRA,isAllInfoForAlbumSavedSuccessfully);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_info_fragment, viewGroup, false);

        int nightModeFlags =
                requireContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;

        boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        Spinner spinner = rootView.findViewById(R.id.album_category);
        String[] album_categories = getAlbumsCategories();
        final List<String> album_categories_list = new ArrayList<>(Arrays.asList(album_categories));
        final ArrayAdapter<String> album_category_adapter = new ArrayAdapter<String>(getContext(),R.layout.spinner_item,album_categories_list){
            @Override
            public boolean isEnabled(int position){
                return position != 0;
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                    if (isNightMode)
                        tv.setTextColor(Color.WHITE);
                }
                return view;
            }
        };

        album_category_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(album_category_adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //String selectedItemText = (String) parent.getItemAtPosition(position);
                mAlbumCategory = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mAlbumNameEditText = rootView.findViewById(R.id.album_name_edt);
        mAlbumDescriptionEditText = rootView.findViewById(R.id.album_description_edt);
        LinearLayout successMessage = rootView.findViewById(R.id.success_message);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ALBUM_NAME_EXTRA)) {
                mAlbumName = savedInstanceState.getString(ALBUM_NAME_EXTRA);
                mAlbumDescriptionEditText.setText(mAlbumName);
            }
            if (savedInstanceState.containsKey(ALBUM_DESCRIPTION_EXTRA)) {
                mAlbumDescription = savedInstanceState.getString(ALBUM_DESCRIPTION_EXTRA);
                mAlbumDescriptionEditText.setText(mAlbumDescription);
            }
            if (savedInstanceState.containsKey(ALBUM_CATEGORY_EXTRA)) {
                mAlbumCategory = savedInstanceState.getInt(ALBUM_CATEGORY_EXTRA);
            }
            if (savedInstanceState.containsKey(ALBUM_INFO_ALL_SET_EXTRA)) {
                isAllInfoForAlbumSavedSuccessfully = savedInstanceState.getBoolean(ALBUM_INFO_ALL_SET_EXTRA);
                if (isAllInfoForAlbumSavedSuccessfully) successMessage.setVisibility(View.VISIBLE);
            }
        }
        Button saveAlbumInfoButton = rootView.findViewById(R.id.saveAlbumInfoButton);
        // save album info button tapped
        saveAlbumInfoButton.setOnClickListener(view -> {
            mAlbumName = mAlbumNameEditText.getText().toString().trim();
            mAlbumDescription = mAlbumDescriptionEditText.getText().toString().trim();
            if (checkForAlbumInfoDataError(mAlbumName,mAlbumDescription)) {
                // error occurred
                successMessage.setVisibility(View.GONE);
                isAllInfoForAlbumSavedSuccessfully = false;
                mMainUtil.clearPreferenceDataByKey(ALBUM_INFO_ALL_SET_EXTRA);
            } else {
                // all album info are ready to be saved
                isAllInfoForAlbumSavedSuccessfully = true;
                successMessage.setVisibility(View.VISIBLE);
                saveAllAlbumInfoDataToPreferences();
                mMainUtil.showToastMessage(getString(R.string.album_info_saved));
            }
        });
        return rootView;
    }

    private boolean checkForAlbumInfoDataError(String albumName, String albumDescription) {
        boolean mIsErrorFound = false;
        // check for album name
        if (albumName.equalsIgnoreCase("")) {
            mAlbumNameEditText.setError(getString(R.string.empty_album_name_error));
            mIsErrorFound = true;
        } else {
            mAlbumNameEditText.setError(null);
        }
        if (mIsErrorFound) return true;
        if (albumName.length() < 5) {
            mAlbumNameEditText.setError(getString(R.string.at_least_5_characters_error));
            mIsErrorFound = true;
        } else {
            mAlbumNameEditText.setError(null);
        }
        if (mIsErrorFound) return true;
        // check for album category
        if (mAlbumCategory == 0) {
            mMainUtil.showToastMessage(getString(R.string.categorie_error_text));
            mIsErrorFound = true;
        }
        if (mIsErrorFound) return true;
        // check for description
        if (albumDescription.equalsIgnoreCase("")) {
            mAlbumDescriptionEditText.setError(getString(R.string.short_description_error));
            mIsErrorFound = true;
        } else {
            mAlbumDescriptionEditText.setError(null);
        }
        return mIsErrorFound;
    }

    public String[] getAlbumsCategories() {
        return new String[]{
                getString(R.string.album_category_head_text),
                getString(R.string.at_home_text),
                getString(R.string.At_work_text),
                getString(R.string.at_gym_text),
                getString(R.string.beach_text),
                getString(R.string.birthday_text),
                getString(R.string.party_text),
                getString(R.string.restaurant_text),
                getString(R.string.night_life_text),
                getString(R.string.travel_text),
                getString(R.string.holiday_text),
                getString(R.string.funeral_text),
                getString(R.string.family_members_text),
                getString(R.string.girlfriend_text),
                getString(R.string.boyfriend),
                getString(R.string.wedding_text),
                getString(R.string.other_text)

        };
    }

    private void saveAllAlbumInfoDataToPreferences() {
        // album name
        mMainUtil.writeDataStringToSharedPreferences(ALBUM_NAME_EXTRA,mAlbumName);
        // album description
        mMainUtil.writeDataStringToSharedPreferences(ALBUM_DESCRIPTION_EXTRA,mAlbumDescription);
        // album category
        mMainUtil.writeDataIntToSharedPreferences(ALBUM_CATEGORY_EXTRA,mAlbumCategory);
        // saved that all album info data has been saved successfully
        mMainUtil.writeDataBooleanToSharedPreferences(ALBUM_INFO_ALL_SET_EXTRA,isAllInfoForAlbumSavedSuccessfully);
    }
}
