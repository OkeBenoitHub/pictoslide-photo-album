package com.pictoslide.www.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.pictoslide.www.R;
import com.pictoslide.www.fragments.AlbumInfoFragment;
import com.pictoslide.www.fragments.AlbumMusicFragment;
import com.pictoslide.www.fragments.AlbumPhotosFragment;

public class AlbumTabsAdapter extends FragmentPagerAdapter {
    private final String[] tabTitles;

    public AlbumTabsAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabTitles = new String[] {
                context.getString(R.string.info_fragment_title_text),
                context.getString(R.string.photos_fragment_title_text),
                context.getString(R.string.music_fragment_title_text)};
    }


    /**
     * Return the {@link Fragment} that should be displayed for the given page number.
     */
    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new AlbumInfoFragment();
        } else if (position == 1) {
            return new AlbumPhotosFragment();
        } else {
            return new AlbumMusicFragment();
        }
    }

    /**
     * Return the total number of pages.
     */
    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}
