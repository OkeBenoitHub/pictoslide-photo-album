package com.pictoslide.www.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;

import com.pictoslide.www.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkUtil {
    private final Context mContext;
    private static String YOUTUBE_SEARCH_API_BASE_URL;
    private final static String API_KEY_PARAM = "key";

    public NetworkUtil(Context context) {
        mContext = context;
        YOUTUBE_SEARCH_API_BASE_URL = "https://www.googleapis.com/youtube/v3/search?";
    }

    /**
     * Build music search Url
     */
    public static URL buildYoutubeSearchUrl(String musicQuery, int maxResult) {
        String API_KEY_VALUE_TEXT = "AIzaSyBTmg-2BF01ha5vp3Kt7d-CEKY2DefXpXQ";
        Uri builtUri = Uri.parse(YOUTUBE_SEARCH_API_BASE_URL).buildUpon()
                .appendQueryParameter("part","snippet")
                .appendQueryParameter("q",musicQuery)
                .appendQueryParameter("videoCategoryId","10")
                .appendQueryParameter("type","video")
                .appendQueryParameter("videoEmbeddable","true")
                .appendQueryParameter("videoSyndicated","true")
                .appendQueryParameter("maxResults",String.valueOf(maxResult))
                .appendQueryParameter(API_KEY_PARAM,API_KEY_VALUE_TEXT)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Check if user is connected to internet
     * @return true or false
     */
    public boolean isUserConnectedToNetwork() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = null;
            if (connectivityManager != null) {
                nw = connectivityManager.getActiveNetwork();
            }
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = null;
            if (connectivityManager != null) {
                nwInfo = connectivityManager.getActiveNetworkInfo();
            }
            return nwInfo != null && nwInfo.isConnectedOrConnecting();
        }
    }

    /**
     * Get internet connectivity status
     * @return status string
     */
    public String getConnectivityStatusString() {
        String status = null;
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                status = mContext.getString(R.string.wifi_enabled_text);
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                status = mContext.getString(R.string.mobile_data_enabled_text);
            }
        } else {
            status = mContext.getString(R.string.No_internet_available_text);
        }
        return status;
    }
}
