/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackDetails implements Parcelable {

    public final String trackName;
    public final String albumName;
    public final String albumArtBigImageUrl;
    public final String albumArtSmallImageUrl;
    public final String previewUrl;

    public TrackDetails(Parcel input) {
        trackName = input.readString();
        albumName = input.readString();
        albumArtBigImageUrl = input.readString();
        albumArtSmallImageUrl = input.readString();
        previewUrl = input.readString();
    }

    public TrackDetails(String trackName, String albumName, String albumArtBigImageUrl, String albumArtSmallImageUrl, String previewUrl) {
        this.trackName = trackName;
        this.albumName = albumName;
        this.albumArtBigImageUrl = albumArtBigImageUrl;
        this.albumArtSmallImageUrl = albumArtSmallImageUrl;
        this.previewUrl = previewUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trackName);
        dest.writeString(albumName);
        dest.writeString(albumArtBigImageUrl);
        dest.writeString(albumArtSmallImageUrl);
        dest.writeString(previewUrl);
    }

    public static final Parcelable.Creator<TrackDetails> CREATOR = new Parcelable.Creator<TrackDetails>() {
        public TrackDetails createFromParcel(Parcel in) {
            return new TrackDetails(in);
        }

        public TrackDetails[] newArray(int size) {
            return new TrackDetails[size];
        }
    };

    @Override
    public String toString() {
        return "trackName: " + trackName + "; albumName: " + albumName + "; albumArtBigImageUrl: " + albumArtBigImageUrl + "; albumArtSmallImageUrl: " + albumArtSmallImageUrl + "; previewUrl: " + previewUrl;
    }
}
