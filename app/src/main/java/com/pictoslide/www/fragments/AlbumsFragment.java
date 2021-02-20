package com.pictoslide.www.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.pictoslide.www.BuildConfig;
import com.pictoslide.www.R;
import com.pictoslide.www.activities.AlbumActivity;
import com.pictoslide.www.activities.AlbumViewerActivity;
import com.pictoslide.www.adapters.AlbumLibAdapter;
import com.pictoslide.www.models.Album;
import com.pictoslide.www.utils.AppConstUtils;
import com.pictoslide.www.utils.MainUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.pictoslide.www.activities.AlbumActivity.ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumActivity.MUSIC_DATA_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumActivity.PHOTO_ITEMS_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumViewerActivity.ALBUM_SUB_OWNERS_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumViewerActivity.OWNER_ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.MainActivity.IS_NEW_ALBUM_EXTRA;
import static com.pictoslide.www.activities.MainActivity.MADE_CHANGES_ALBUM_LIBRARY;

public class AlbumsFragment extends Fragment implements AlbumLibAdapter.ListItemClickListener {
    private MainUtil mMainUtil;
    private FirebaseFirestore mFirebaseFirestore;
    private ArrayList<Album> mAlbumItems;
    private ArrayList<Album> mSearchAlbumsResults;
    private AlbumLibAdapter mAlbumLibAdapter;
    private AlbumLibAdapter mAlbumLibAdapterByCategory;
    private RecyclerView albums_lib_recycler_list;
    private RecyclerView albums_lib_recycler_list_by_category;

    private String mCurrentUserId;
    private ProgressBar mProgressBar;
    private TextView mErrorMessage;
    private String mAlbumNameSearchValue;
    private ArrayList<Album> mAlbumItemsByCategory;
    private TextView no_albums_found_by_category;

    /**
     * Swipe callback on recyclerview albums items
     */
    private final ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //Remove swiped item from list and notify the RecyclerView
            int position = viewHolder.getAdapterPosition();
            mAlbumLibAdapter.removeAlbumFromLibraryByPosition(position, shouldRemoveAlbumFromLibrary -> {
                mAlbumItems.remove(position);
                mAlbumLibAdapter.swapAlbums(mAlbumItems);
                if (!shouldRemoveAlbumFromLibrary) {
                    NavHostFragment.findNavController(AlbumsFragment.this).navigateUp();
                    mMainUtil.writeDataBooleanToSharedPreferences(MADE_CHANGES_ALBUM_LIBRARY,true);
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainUtil = new MainUtil(requireContext());
        mFirebaseFirestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        mCurrentUserId = mMainUtil.getUniqueID(requireContext());
        albums_lib_recycler_list = rootView.findViewById(R.id.my_albums_recycler_list);
        albums_lib_recycler_list_by_category = rootView.findViewById(R.id.my_albums_category_recycler_list);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        albums_lib_recycler_list.setLayoutManager(new LinearLayoutManager(requireContext()));
        albums_lib_recycler_list_by_category.setLayoutManager(new LinearLayoutManager(requireContext()));

        mAlbumItems = new ArrayList<>();
        mAlbumItemsByCategory = new ArrayList<>();

        // Initialize the adapter and attach it to the RecyclerView
        mAlbumLibAdapter = new AlbumLibAdapter(requireContext(), mAlbumItems, this);
        albums_lib_recycler_list.setAdapter(mAlbumLibAdapter);
        // attach swipe to delete to recyclerview of albums
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(albums_lib_recycler_list);

        mAlbumLibAdapterByCategory = new AlbumLibAdapter(requireContext(),mAlbumItemsByCategory, this);
        albums_lib_recycler_list_by_category.setAdapter(mAlbumLibAdapterByCategory);

        DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        albums_lib_recycler_list.addItemDecoration(decoration);
        albums_lib_recycler_list_by_category.addItemDecoration(decoration);

        mProgressBar = rootView.findViewById(R.id.loader_bar);
        mErrorMessage = rootView.findViewById(R.id.error_found_tv);

        no_albums_found_by_category = rootView.findViewById(R.id.no_albums_by_category_found_tv);
        no_albums_found_by_category.setText(getString(R.string.sort_albums_by_category_head_text));

        mProgressBar.setVisibility(View.VISIBLE);
        loadAlbumsDataFromDb();
        return rootView;
    }

    private void loadAlbumsDataFromDb() {
        mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME)
                .orderBy("createdAtTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }
                    getQuerySnapshotAlbumsData(snapshot);
                });

    }

    private void getQuerySnapshotAlbumsData(QuerySnapshot snapshot) {
        if (snapshot != null) {
            //Log.d(TAG, "Current data: " + snapshot.getData());
            if (!snapshot.isEmpty()) {
                mAlbumItems.clear();
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    Album albumItem = document.toObject(Album.class);
                    List<String> albumSubOwnersId = null;
                    if (albumItem != null) {
                        albumSubOwnersId = albumItem.getSubOwnersId();
                    }
                    boolean isUserAlbumSubOwner = false;
                    if (albumSubOwnersId != null && albumSubOwnersId.contains(mCurrentUserId)) {
                        isUserAlbumSubOwner = true;
                    }
                    if (albumItem != null && (albumItem.getOwnerId().equals(mCurrentUserId) || isUserAlbumSubOwner)) {
                        mAlbumItems.add(albumItem);
                    }
                }
                if (!mAlbumItems.isEmpty()) {
                    mAlbumLibAdapter.swapAlbums(mAlbumItems);
                    showAlbumsData();
                } else {
                    // no albums found
                    showErrorMessage(getString(R.string.no_album_found_label_text));
                }
            } else {
                // no data found
                showErrorMessage(getString(R.string.no_album_found_label_text));
            }
        } else {
            // no data found
            showErrorMessage(getString(R.string.no_album_found_label_text));
        }
    }

    private void showAlbumsData() {
        albums_lib_recycler_list.setVisibility(View.VISIBLE);
        mErrorMessage.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void showErrorMessage(String errorMessage) {
        albums_lib_recycler_list.setVisibility(View.INVISIBLE);
        mErrorMessage.setVisibility(View.VISIBLE);
        mErrorMessage.setText(errorMessage);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // newAlbumFabButton tapped
        view.findViewById(R.id.new_album_fab_button).setOnClickListener(view1 -> {
            Intent intent = new Intent(getContext(), AlbumActivity.class);
            intent.putExtra(IS_NEW_ALBUM_EXTRA, true);
            startActivity(intent);
        });

        // sortAlbumsByButton tapped
        view.findViewById(R.id.sortAlbumsByButton).setOnClickListener(view12 -> showSortAlbumsByCategoriesBox(view));

        // search albums button tapped
        view.findViewById(R.id.searchAlbumsButton).setOnClickListener(view15 -> showSearchAlbumsBoxLayout(view));

        /*
         * Search for albums by name
         */
        EditText search_album_name_edit_text = view.findViewById(R.id.album_name_search_edt);
        RecyclerView searchAlbumsRecyclerView = view.findViewById(R.id.albums_recycler_list_search_results);
        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        searchAlbumsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mSearchAlbumsResults = new ArrayList<>();
        // Initialize the adapter and attach it to the RecyclerView
        mAlbumLibAdapter = new AlbumLibAdapter(requireContext(),mSearchAlbumsResults,this);
        searchAlbumsRecyclerView.setAdapter(mAlbumLibAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        searchAlbumsRecyclerView.addItemDecoration(decoration);
        TextView noAlbumsFoundTv = view.findViewById(R.id.error_found_tv_2);
        search_album_name_edit_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                mAlbumNameSearchValue = editable.toString().toLowerCase().trim();
                if (!mAlbumNameSearchValue.isEmpty()) {
                    if (!mAlbumItems.isEmpty()) {
                        // do some search
                        int albumsFound = 0;
                        mSearchAlbumsResults.clear();
                        for (int i = 0; i < mAlbumItems.size(); i++) {
                            if (mAlbumItems.get(i).getName().toLowerCase().contains(mAlbumNameSearchValue)) {
                                albumsFound++;
                                mSearchAlbumsResults.add(mAlbumItems.get(i));
                            }
                        }
                        if (albumsFound > 0) {
                            searchAlbumsRecyclerView.setVisibility(View.VISIBLE);
                            noAlbumsFoundTv.setVisibility(View.GONE);
                            mAlbumLibAdapter.swapAlbums(mSearchAlbumsResults);
                        } else {
                            // no results
                            searchAlbumsRecyclerView.setVisibility(View.INVISIBLE);
                            noAlbumsFoundTv.setText(R.string.no_search_albums_found_tv);
                            noAlbumsFoundTv.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // no results
                        searchAlbumsRecyclerView.setVisibility(View.INVISIBLE);
                        noAlbumsFoundTv.setText(R.string.no_search_albums_found_tv);
                        noAlbumsFoundTv.setVisibility(View.VISIBLE);
                    }
                } else {
                    // no results
                    searchAlbumsRecyclerView.setVisibility(View.INVISIBLE);
                    noAlbumsFoundTv.setText(R.string.find_album_by_name_text);
                    noAlbumsFoundTv.setVisibility(View.VISIBLE);
                }
            }
        });

        view.findViewById(R.id.closeSearchAlbumsBox).setOnClickListener(view16 -> hideSearchAlbumsBoxLayout(view));

        view.findViewById(R.id.closeSortAlbumsButton).setOnClickListener(view13 -> hideSortAlbumsByCategoriesBox(view));

        /* Set up spinner for album categories */
        Spinner spinner = view.findViewById(R.id.album_category);
        String[] album_categories = getAlbumsCategories();
        final List<String> album_categories_list = new ArrayList<>(Arrays.asList(album_categories));
        final ArrayAdapter<String> album_category_adapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item, album_categories_list) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                int nightModeFlags =
                        requireContext().getResources().getConfiguration().uiMode &
                                Configuration.UI_MODE_NIGHT_MASK;

                boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                if (position == 0) {
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
                if (position > 0) {
                    sortAlbumsDataByCategory(position);
                } else {
                    albums_lib_recycler_list_by_category.setVisibility(View.INVISIBLE);
                    no_albums_found_by_category.setVisibility(View.VISIBLE);
                    no_albums_found_by_category.setText(getString(R.string.sort_albums_by_category_head_text));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void sortAlbumsDataByCategory(int albumCategory) {
        mAlbumItemsByCategory.clear();
        if (!mAlbumItems.isEmpty()) {
            for (Album albumItem: mAlbumItems) {
                 if (albumItem.getCategory() == albumCategory) {
                     mAlbumItemsByCategory.add(albumItem);
                 }
            }
            if (!mAlbumItemsByCategory.isEmpty()) {
                mAlbumLibAdapterByCategory.swapAlbums(mAlbumItemsByCategory);
                albums_lib_recycler_list_by_category.setVisibility(View.VISIBLE);
                no_albums_found_by_category.setVisibility(View.INVISIBLE);
            } else {
                albums_lib_recycler_list_by_category.setVisibility(View.INVISIBLE);
                no_albums_found_by_category.setVisibility(View.VISIBLE);
                // no albums data for this category
                no_albums_found_by_category.setText(getString(R.string.no_album_found_for_category_label_text));
            }
        } else {
            albums_lib_recycler_list_by_category.setVisibility(View.INVISIBLE);
            no_albums_found_by_category.setVisibility(View.VISIBLE);
            if (albumCategory > 0) {
                // no albums data for this category
                no_albums_found_by_category.setText(getString(R.string.no_album_found_for_category_label_text));
            } else {
                no_albums_found_by_category.setText(getString(R.string.sort_albums_by_category_head_text));
            }
        }
    }

    private void showSortAlbumsByCategoriesBox(View view) {
        CardView topHeaderAlbumBoxLayout = view.findViewById(R.id.top_library_header);
        topHeaderAlbumBoxLayout.setVisibility(View.INVISIBLE);
        RelativeLayout sortAlbumsByCategoriesBox = view.findViewById(R.id.sort_albums_by_categories_box);
        // show albums by categories box
        sortAlbumsByCategoriesBox.setVisibility(View.VISIBLE);
        view.findViewById(R.id.new_album_fab_button).setVisibility(View.INVISIBLE);
    }

    private void showSearchAlbumsBoxLayout(View view) {
        CardView topHeaderAlbumBoxLayout = view.findViewById(R.id.top_library_header);
        topHeaderAlbumBoxLayout.setVisibility(View.INVISIBLE);
        RelativeLayout searchAlbumsBoxLayout = view.findViewById(R.id.search_albums_box_layout);
        // show search albums box layout
        searchAlbumsBoxLayout.setVisibility(View.VISIBLE);
        view.findViewById(R.id.new_album_fab_button).setVisibility(View.INVISIBLE);
    }

    private void hideSearchAlbumsBoxLayout(View view) {
        CardView topHeaderAlbumBoxLayout = view.findViewById(R.id.top_library_header);
        topHeaderAlbumBoxLayout.setVisibility(View.VISIBLE);
        RelativeLayout searchAlbumsBoxLayout = view.findViewById(R.id.search_albums_box_layout);
        // hide search albums box
        searchAlbumsBoxLayout.setVisibility(View.INVISIBLE);
        view.findViewById(R.id.new_album_fab_button).setVisibility(View.VISIBLE);

    }

    private void hideSortAlbumsByCategoriesBox(View view) {
        CardView topHeaderAlbumBoxLayout = view.findViewById(R.id.top_library_header);
        topHeaderAlbumBoxLayout.setVisibility(View.VISIBLE);
        RelativeLayout sortAlbumsByCategoriesBox = view.findViewById(R.id.sort_albums_by_categories_box);
        // hide albums by categories box
        sortAlbumsByCategoriesBox.setVisibility(View.INVISIBLE);
        view.findViewById(R.id.new_album_fab_button).setVisibility(View.VISIBLE);
        no_albums_found_by_category.setText(getString(R.string.sort_albums_by_category_head_text));
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

    @Override
    public void onListItemClick(int clickedItemIndex) {
        Intent intent = new Intent(requireContext(),AlbumViewerActivity.class);
        Album album = mAlbumItems.get(clickedItemIndex);
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
        intent.putExtra(IS_NEW_ALBUM_EXTRA,true);
        intent.putExtra(OWNER_ALBUM_ID_EXTRA,album.getOwnerId());
        intent.putExtra(ALBUM_ID_EXTRA,album.getAlbumId());
        startActivity(intent);
    }

    @Override
    public void onShareAlbumItem(int clickedItemIndex) {
           Album albumItem = mAlbumItems.get(clickedItemIndex);
           if (albumItem != null) {
               shareAlbumInLibrary(albumItem);
           }
    }

    private void shareAlbumInLibrary(Album album) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://www.pictoslideapp.com/albumId/" + album.getAlbumId()))
                .setDomainUriPrefix("https://pictoslideapp.page.link")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).setMinimumVersion(1).build())
                // Set parameters
                // ...
                .buildShortDynamicLink()
                .addOnCompleteListener(requireActivity(), task -> {
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
}