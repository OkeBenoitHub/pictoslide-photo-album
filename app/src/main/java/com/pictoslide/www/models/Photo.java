package com.pictoslide.www.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Photo implements Parcelable {
    private final String absolutePath;
    private final String caption;

    public Photo(String absolutePath,String caption) {
        this.absolutePath = absolutePath;
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    private Photo(Parcel in) {
        absolutePath = in.readString();
        caption = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(absolutePath);
        out.writeString(caption);
    }

    public static final Creator<Photo> CREATOR = new Creator<Photo>() {
        public Photo createFromParcel(Parcel in) {
            return new Photo(in);
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };
}
