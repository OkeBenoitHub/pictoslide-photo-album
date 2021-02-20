package com.pictoslide.www.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pictoslide.www.R;
import com.pictoslide.www.activities.YoutubePlayerActivity;
import com.pictoslide.www.adapters.MusicAdapter;
import com.pictoslide.www.models.Music;
import com.pictoslide.www.utils.MainUtil;
import com.pictoslide.www.utils.NetworkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class AlbumMusicFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {
    private MainUtil mMainUtil;
    private NetworkUtil mNetworkUtil;
    private static final int LAUNCH_YOUTUBE_PLAYER_ACTIVITY = 1;
    public static final String MUSIC_ITEM_ADD_TO_ALBUM_ACTION = "1";
    public static final String MUSIC_ITEM_REMOVE_FROM_ALBUM_ACTION = "-1";
    private static final String MUSIC_DATA_LIST_PREFS = "music_data_list_extra";
    private RelativeLayout mMusicSearchBox;

    private static final int YOUTUBE_SEARCH_API_LOADER = 1;
    private static final String MUSIC_RESULTS_LIST_EXTRA = "music_results_list_extra" ;
    private static final String MUSIC_ALBUM_LIST_EXTRA = "music_album_list_extra" ;
    private EditText mMusicQueryEditText;
    private TextView mErrorMessageTV;
    private TextView mEmptyMusicTV;
    private RecyclerView mMusicResultsRecyclerList;
    private RecyclerView mMusicAlbumRecyclerList;
    private ProgressBar mProgressBar;

    private static final String MUSIC_SEARCH_QUERY_EXTRA = "search_query_extra";
    private static final String MAX_RESULT_QUERY_EXTRA = "max_result_query_extra";

    private ArrayList<Music> mMusicResultsArrayList;
    private ArrayList<Music> mMusicAlbumArrayList;
    private MusicAdapter mMusicAdapter;
    private ArrayList<String> mMusicIdsList;

    //private String mMusicSearchQueryValue;
    private Intent mIntent;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mMainUtil = new MainUtil(requireContext());
        mNetworkUtil = new NetworkUtil(getContext());
        mMusicResultsArrayList = new ArrayList<>();
        mMusicAlbumArrayList = new ArrayList<>();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMusicResultsArrayList.size() > 0) {
            outState.putParcelableArrayList(MUSIC_RESULTS_LIST_EXTRA,mMusicResultsArrayList);
        }
        if (mMusicAlbumArrayList.size() > 0) {
            outState.putParcelableArrayList(MUSIC_ALBUM_LIST_EXTRA,mMusicAlbumArrayList);
        }
    }

    private void showErrorMessage(){
        mErrorMessageTV.setVisibility(View.VISIBLE);
        mMusicResultsRecyclerList.setVisibility(View.INVISIBLE);
    }

    private void showMusicResultsData(){
        mErrorMessageTV.setVisibility(View.INVISIBLE);
        mMusicResultsRecyclerList.setVisibility(View.VISIBLE);
    }

    private void showYoutubeMusicResultsFound(String musicSearchQueryValue) {
        if (mNetworkUtil.isUserConnectedToNetwork()) {
            Bundle queryBundle = new Bundle();
            queryBundle.putString(MUSIC_SEARCH_QUERY_EXTRA, musicSearchQueryValue);
            queryBundle.putInt(MAX_RESULT_QUERY_EXTRA, 15);
            // Call getSupportLoaderManager and store it in a LoaderManager variable
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            // Get our Loader by calling getLoader and passing the ID we specified
            Loader<String> loader = loaderManager.getLoader(YOUTUBE_SEARCH_API_LOADER);
            // If the Loader was null, initialize it. Else, restart it.
            if (loader == null) {
                loaderManager.initLoader(YOUTUBE_SEARCH_API_LOADER, queryBundle, this);
            } else {
                loaderManager.restartLoader(YOUTUBE_SEARCH_API_LOADER, queryBundle, this);
            }
        } else {
            // not connected to internet
            mErrorMessageTV.setText(getString(R.string.No_internet_available_text));
            showErrorMessage();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_music_fragment, viewGroup, false);
        mMusicSearchBox = rootView.findViewById(R.id.music_search_box);
        ImageView mCloseMusicBoxButton = rootView.findViewById(R.id.closeMusicSearchBoxButton);

        mCoordinatorLayout = rootView.findViewById(R.id.coordinatorLayout);

        // add music button tapped
        FloatingActionButton addMusicButton = rootView.findViewById(R.id.add_music_floating_action_button);
        addMusicButton.setOnClickListener(v -> mMusicSearchBox.setVisibility(View.VISIBLE));

        // close music box tapped
        mCloseMusicBoxButton.setOnClickListener(v -> mMusicSearchBox.setVisibility(View.INVISIBLE));

        mMusicQueryEditText = rootView.findViewById(R.id.music_query_edit_text);
        mMusicQueryEditText.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && keyCode == KeyEvent.KEYCODE_ENTER) {
                String musicQueryEditTextValue = mMusicQueryEditText.getText().toString().trim();
                if (!musicQueryEditTextValue.equalsIgnoreCase("")) {
                    //mMusicSearchQueryValue = musicQueryEditTextValue;
                    showYoutubeMusicResultsFound(musicQueryEditTextValue);
                }
                return true;
            }
            return false;
        });

        // search music button tapped
        ImageView mSearchMusicButton = rootView.findViewById(R.id.searchMusicButton);
        mSearchMusicButton.setOnClickListener(v -> {
            String musicQueryEditTextValue = mMusicQueryEditText.getText().toString().trim();
            if (!musicQueryEditTextValue.equalsIgnoreCase("")) {
                //mMusicSearchQueryValue = musicQueryEditTextValue;
                showYoutubeMusicResultsFound(musicQueryEditTextValue);
            }
        });

        mErrorMessageTV = rootView.findViewById(R.id.error_text_view);
        mEmptyMusicTV = rootView.findViewById(R.id.empty_music_view);
        mMusicResultsRecyclerList = rootView.findViewById(R.id.music_results_recycler_view);
        mMusicAlbumRecyclerList = rootView.findViewById(R.id.recycler_view_music_album);
        mProgressBar = rootView.findViewById(R.id.progress_circular);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(MUSIC_RESULTS_LIST_EXTRA)) {
                mMusicResultsArrayList = savedInstanceState.getParcelableArrayList(MUSIC_RESULTS_LIST_EXTRA);
            }
            if (savedInstanceState.containsKey(MUSIC_ALBUM_LIST_EXTRA)) {
                mMusicAlbumArrayList = savedInstanceState.getParcelableArrayList(MUSIC_ALBUM_LIST_EXTRA);
            }
        }
        setUpMusicResultFoundAdapter(mMusicResultsArrayList);
        setUpMusicAlbumAdapter(mMusicAlbumArrayList);
        return rootView;
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        // Here we will initiate AsyncTaskLoader and handle task in background
        return new AsyncTaskLoader<String>(requireContext()) {
            String musicFoundResults;
            @Override
            public String loadInBackground() {
                String searchQueryExtra = null;
                if (args != null) {
                    searchQueryExtra = args.getString(MUSIC_SEARCH_QUERY_EXTRA);
                }
                int maxResultExtra = 0;
                if (args != null) {
                    maxResultExtra = args.getInt(MAX_RESULT_QUERY_EXTRA);
                }
                // Think of this as AsyncTask doInBackground() method, here we will actually initiate Network call, or any work that need to be done on background
                URL YoutubeSearchApiUrl = NetworkUtil.buildYoutubeSearchUrl(searchQueryExtra,maxResultExtra);
                try {
                    musicFoundResults = NetworkUtil.getResponseFromHttpUrl(YoutubeSearchApiUrl); // This just create a HTTPUrlConnection and return result in strings
                    return musicFoundResults;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onStartLoading() {
                if (musicFoundResults != null) {
                    // To skip loadInBackground call
                    deliverResult(musicFoundResults);
                } else {
                    mMusicResultsRecyclerList.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public void deliverResult(String data) {
                musicFoundResults = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        mProgressBar.setVisibility(View.INVISIBLE);
        showMusicResultsData();
        if (data != null && !data.equals("")) {
            String music_title = "empty";
            String music_video_id = "empty";
            String music_published_date = "empty";
            String music_thumbnail_path = "empty";
            mMusicResultsArrayList = new ArrayList<>();
            try {
                JSONObject musicJsonRootObject = new JSONObject(data);
                if (musicJsonRootObject.has("items")) {
                    JSONArray musicItemsJsonArray = musicJsonRootObject.optJSONArray("items");

                    if (musicItemsJsonArray != null) {
                        if (musicItemsJsonArray.length() > 0) {
                            for (int i = 0; i < musicItemsJsonArray.length(); i++) {
                                JSONObject musicItemJsonObject = musicItemsJsonArray.optJSONObject(i);
                                if (musicItemJsonObject.has("id")) {
                                    JSONObject musicItemIdJsonObject = musicItemJsonObject.optJSONObject("id");
                                    if (musicItemIdJsonObject != null && musicItemIdJsonObject.has("videoId")) {
                                        music_video_id = musicItemIdJsonObject.optString("videoId");
                                    }
                                }
                                if (musicItemJsonObject.has("snippet")) {
                                    JSONObject musicItemSnippetJsonObject = musicItemJsonObject.optJSONObject("snippet");
                                    if (musicItemSnippetJsonObject != null && musicItemSnippetJsonObject.has("publishedAt")) {
                                        music_published_date = musicItemSnippetJsonObject.optString("publishedAt");
                                        music_published_date = music_published_date.substring(0, 4);
                                    }
                                    if (musicItemSnippetJsonObject != null && musicItemSnippetJsonObject.has("title")) {
                                        music_title = musicItemSnippetJsonObject.optString("title");
                                        music_title = String.valueOf(Html.fromHtml(music_title));
                                    }
                                    if (musicItemSnippetJsonObject != null && musicItemSnippetJsonObject.has("thumbnails")) {
                                        JSONObject musicItemThumbnailJsonObject = musicItemSnippetJsonObject.optJSONObject("thumbnails");
                                        if (musicItemThumbnailJsonObject != null && musicItemThumbnailJsonObject.has("medium")) {
                                            JSONObject musicItemDefaultThumbnailJsonObject = musicItemThumbnailJsonObject.optJSONObject("medium");
                                            if (musicItemDefaultThumbnailJsonObject != null && musicItemDefaultThumbnailJsonObject.has("url")) {
                                                music_thumbnail_path = musicItemDefaultThumbnailJsonObject.optString("url");
                                            }
                                        }
                                    }
                                }
                                Music musicItem = new Music(music_title, music_video_id, music_thumbnail_path, music_published_date);
                                // add a new music item to array list
                                mMusicResultsArrayList.add(musicItem);
                            }
                            setUpMusicResultFoundAdapter(mMusicResultsArrayList);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mErrorMessageTV.setText(getString(R.string.no_music_found));
            showErrorMessage();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_YOUTUBE_PLAYER_ACTIVITY) {
            if(resultCode == Activity.RESULT_OK){
                int music_intent_position = data.getIntExtra(YoutubePlayerActivity.MUSIC_ITEM_POSITION_EXTRA,-1);
                String music_intent_perform_action = data.getStringExtra(YoutubePlayerActivity.MUSIC_ITEM_PERFORM_ACTION_EXTRA);
                if (music_intent_position != -1) {
                    Music musicIntentItem = mMusicResultsArrayList.get(music_intent_position);
                    if (music_intent_perform_action.equalsIgnoreCase(MUSIC_ITEM_ADD_TO_ALBUM_ACTION)) {
                        mMusicAlbumArrayList.add(musicIntentItem);
                        saveAllMusicIdsListToPreferences();
                        mMainUtil.displaySnackBarMessage(mCoordinatorLayout,R.string.added_successfully, Snackbar.LENGTH_LONG);
                    } else if (music_intent_perform_action.equalsIgnoreCase(MUSIC_ITEM_REMOVE_FROM_ALBUM_ACTION)) {
                        mMusicAlbumArrayList.remove(music_intent_position);
                        saveAllMusicIdsListToPreferences();
                        mMainUtil.showToastMessage(getString(R.string.removed_successfully));
                    }
                    mMusicSearchBox.setVisibility(View.INVISIBLE);
                    //mMusicAdapter.swapMusic(mMusicAlbumArrayList);
                    setUpMusicAlbumAdapter(mMusicAlbumArrayList);
                    if (mMusicAlbumArrayList.size() <= 0) {
                        mEmptyMusicTV.setVisibility(View.VISIBLE);
                        mMusicAlbumRecyclerList.setVisibility(View.INVISIBLE);
                    } else {
                        mEmptyMusicTV.setVisibility(View.INVISIBLE);
                        mMusicAlbumRecyclerList.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private void setUpMusicResultFoundAdapter(ArrayList<Music> musicResultsFoundArrayList) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mMusicResultsRecyclerList.setLayoutManager(layoutManager);
        mMusicResultsRecyclerList.setHasFixedSize(true);
        mMusicAdapter = new MusicAdapter(getContext(), musicResultsFoundArrayList, clickedItemIndex -> {
            Music musicItem = musicResultsFoundArrayList.get(clickedItemIndex);
            if (musicItem.getVideoId().length() == 11) {
                mIntent = new Intent(getContext(), YoutubePlayerActivity.class);
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_ID_EXTRA,musicItem.getVideoId());
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_POSITION_EXTRA,clickedItemIndex);
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_PERFORM_ACTION_EXTRA,MUSIC_ITEM_ADD_TO_ALBUM_ACTION);
                startActivityForResult(mIntent, LAUNCH_YOUTUBE_PLAYER_ACTIVITY);
            }
        });
        mMusicResultsRecyclerList.setAdapter(mMusicAdapter);
        if (musicResultsFoundArrayList.size() <= 0) {
            showErrorMessage();
        } else {
            showMusicResultsData();
        }
    }

    private void setUpMusicAlbumAdapter(ArrayList<Music> musicAlbumArrayList) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mMusicAlbumRecyclerList.setLayoutManager(layoutManager);
        mMusicAlbumRecyclerList.setHasFixedSize(true);
        mMusicAdapter = new MusicAdapter(getContext(), musicAlbumArrayList, clickedItemIndex -> {
            Music musicItem = musicAlbumArrayList.get(clickedItemIndex);
            if (musicItem.getVideoId().length() == 11) {
                mIntent = new Intent(getContext(), YoutubePlayerActivity.class);
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_ID_EXTRA,musicItem.getVideoId());
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_POSITION_EXTRA,clickedItemIndex);
                mIntent.putExtra(YoutubePlayerActivity.MUSIC_ITEM_PERFORM_ACTION_EXTRA,MUSIC_ITEM_REMOVE_FROM_ALBUM_ACTION);
                startActivityForResult(mIntent, LAUNCH_YOUTUBE_PLAYER_ACTIVITY);
            }
        });
        mMusicAlbumRecyclerList.setAdapter(mMusicAdapter);
        if (musicAlbumArrayList.size() <= 0) {
            mEmptyMusicTV.setVisibility(View.VISIBLE);
            mMusicAlbumRecyclerList.setVisibility(View.INVISIBLE);
        } else {
            mEmptyMusicTV.setVisibility(View.INVISIBLE);
            mMusicAlbumRecyclerList.setVisibility(View.VISIBLE);
        }
    }

    public ArrayList<Music> getMusicAlbumArrayList() {
        return mMusicAlbumArrayList;
    }

    public ArrayList<String> getAllMusicIdsList() {
        return mMusicIdsList;
    }

    private void setAllMusicIdsList() {
        mMusicIdsList = new ArrayList<>();
        if (getMusicAlbumArrayList().size() > 0) {
            for (int i = 0; i < getMusicAlbumArrayList().size(); i++) {
                mMusicIdsList.add(getMusicAlbumArrayList().get(i).getVideoId() + "|" + getMusicAlbumArrayList().get(i).getTitle());
            }
        }
    }

    private void saveAllMusicIdsListToPreferences() {
        setAllMusicIdsList();
        mMainUtil.clearPreferenceDataByKey(MUSIC_DATA_LIST_PREFS);
        mMainUtil.writeDataArrayListStringToSharedPreferences(MUSIC_DATA_LIST_PREFS,getAllMusicIdsList());
    }
}
