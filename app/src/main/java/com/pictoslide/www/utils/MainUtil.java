package com.pictoslide.www.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ShareCompat;

import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.google.android.material.snackbar.Snackbar;
import com.pictoslide.www.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainUtil {
    private final Context mContext;
    private String uniqueID = null;
    private Toast mToast;
    private final SharedPreferences mSharedPreferences;

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public MainUtil(Context context) {
        mContext = context;
        DateTimeUtils.setTimeZone("UTC");
        mSharedPreferences = mContext.getSharedPreferences(
                context.getString(R.string.package_name_text), Context.MODE_PRIVATE);
    }

    /**
     * Get time ago based on timestamp from past
     * @param time :: timestamp
     * @return :: explicit time ago
     */
    public static String getTimeAgo(long time,Context context) {
        if (time < 1000000000000L) {
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return context.getResources().getString(R.string.just_now_ago_text);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return context.getResources().getString(R.string.minute_ago_text);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " " + context.getResources().getString(R.string.minutes_ago_text);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return context.getResources().getString(R.string.hour_ago_text);
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " " + context.getResources().getString(R.string.hours_ago_text);
        } else if (diff < 48 * HOUR_MILLIS) {
            return context.getResources().getString(R.string.yesterday_text);
        } else {
            return diff / DAY_MILLIS + " " + context.getResources().getString(R.string.days_ago_text);
        }
    }

    /*
     * Show snack bar message
     */
    public void showSnackBarMessage(View contextView, String message) {
        String messageBox = "<font color='#94af76'>" + message + "</font>";
        Snackbar snackbar = Snackbar.make(contextView, Html.fromHtml(messageBox), Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(mContext.getResources().getColor(R.color.white));
        snackbar.show();
    }

    /**
     * Display snack bar message
     * @param contextView :: coordinatorLayout root view
     * @param messageResId :: message resource id string
     */
    public Snackbar displaySnackBarMessage(CoordinatorLayout contextView, int messageResId, int snackBarDuration) {
        Snackbar snackbar = Snackbar.make(contextView,
                messageResId, snackBarDuration);
        snackbar.show();
        return snackbar;
    }

    /**
     * Show toast message
     * @param message :: message to show
     */
    public void showToastMessage(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mContext,message,Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.CENTER,0,0);
        mToast.show();
    }

    /**
     * This method is used for checking valid email id format.
     *
     * @param email to check for
     * @return boolean true for valid false for invalid
     */
    public boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }


    /*
    public void loadBannerAdFromADMOB(AdView adView) {
        MobileAds.initialize(mContext, initializationStatus -> { });
        adView.setVisibility(View.GONE);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }*/

    /**
     * Compose email intent
     * @param addresses :: address to send email to
     * @param subject :: email subject
     */
    public void composeEmail(String[] addresses, String subject, String message, String sharerTitle) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT,message);
        Intent chooser = Intent.createChooser(intent,sharerTitle);
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(chooser);
        }
    }

    /**
     *
     * @param stringInput :: the string to transform
     * @return new string with each word capitalized
     */
    public String capitalizeEachWordFromString(String stringInput){
        String[] strArray = stringInput.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap).append(" ");
        }
        return builder.toString();
    }

    /*
     * This method checks for a valid name
     * @param name :: name to check for
     * @return true or false
     */
    /*
    public boolean isValidName(String name) {
        String nameRegX = "^[\\p{L} .'-]+$";
        return name.matches(nameRegX);
    }*/

    /**
     * Open a specific web page
     * @param url :: page address url
     */
    public void openWebPage(String url) {
        Uri webPage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webPage);

        /*
         * This is a check we perform with every implicit Intent that we launch. In some cases,
         * the device where this code is running might not have an Activity to perform the action
         * with the data we've specified. Without this check, in those cases your app would crash.
         */
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(intent);
        }
    }

    /**
     * Share Text data through app
     * @param shareAboutTitle :: title of share dialog
     * @param textToShare :: text data to share
     */
    public void shareTextData(String shareAboutTitle, String textToShare) {
        String mimeType = "text/plain";

        // Use ShareCompat.IntentBuilder to build the Intent and start the chooser
        /* ShareCompat.IntentBuilder provides a fluent API for creating Intents */
        ShareCompat.IntentBuilder
                /* The from method specifies the Context from which this share is coming from */
                .from((Activity) mContext)
                .setType(mimeType)
                .setChooserTitle(shareAboutTitle)
                .setText(textToShare)
                .startChooser();
    }

    /**
     * Get unique android device id
     * @param context :: context
     * @return :: return unique id
     */
    public synchronized String getUniqueID(Context context) {
        if (uniqueID == null) {
            String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.apply();
            }
        }
        return uniqueID;
    }

    /**
     * Write string data to preferences
     * @param keyValue :: key value
     * @param value :: value to be stored
     */
    public void writeDataStringToSharedPreferences(String keyValue, String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(keyValue,value);
        editor.apply();
    }

    /**
     * Get string data from preferences
     * @param keyValue :: key value
     * @return data string
     */
    public String getDataStringFromSharedPreferences(String keyValue) {
        return mSharedPreferences.getString(keyValue,"");
    }

    /**
     * Write int data to preferences
     * @param keyValue :: data key value
     * @param value :: int value to be stored
     */
    public void writeDataIntToSharedPreferences(String keyValue, int value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(keyValue,value);
        editor.apply();
    }

    /**
     * Get int data from preferences
     * @param keyValue :: key for preference
     * @return data int
     */
    public int getDataIntFromSharedPreferences(String keyValue) {
        return mSharedPreferences.getInt(keyValue,0);
    }

    /**
     * Write boolean data to preferences
     * @param keyValue :: boolean key data
     * @param value :: boolean data to be stored
     */
    public void writeDataBooleanToSharedPreferences(String keyValue, boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(keyValue,value);
        editor.apply();
    }

    /**
     * Get boolean data from preferences
     * @param keyValue :: key for preference
     * @return boolean data
     */
    public boolean getDataBooleanFromSharedPreferences(String keyValue) {
        return mSharedPreferences.getBoolean(keyValue,false);
    }

    /**
     * Write array list data to preferences
     * @param keyValue :: key of array list data
     * @param arrayListValue :: array list value to be stored
     */
    public void writeDataArrayListStringToSharedPreferences(String keyValue, ArrayList<String> arrayListValue) {
        Set<String> setValue = new HashSet<>();
        setValue.addAll(arrayListValue);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putStringSet(keyValue,setValue);
        editor.apply();
    }

    /**
     * Get array list data from preferences
     * @param keyValue :: key for preference
     * @return array list data
     */
    public Set<String> getDataArrayListStringFromSharedPreferences(String keyValue) {
        return mSharedPreferences.getStringSet(keyValue,null);
    }

    /**
     * Delete all data from preferences
     */
    public void clearAllPreferencesData() {
        mSharedPreferences.edit().clear().apply();
    }

    /**
     * Delete a specific preference by key value
     * @param keyName :: key for preference
     */
    public void clearPreferenceDataByKey(String keyName) {
        mSharedPreferences.edit().remove(keyName).apply();
    }

    /*
    public void writeDataArrayListObjectToSharedPreferences(String keyValue, List<BillEntry> arrayListValue) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayListValue);
        editor.putString(keyValue, json);
        editor.apply();
    } */

}
