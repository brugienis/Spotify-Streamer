package au.com.kbrsolutions.spotifystreamer.core;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by business on 9/06/2015.
 */
public class ArtistDetails implements Parcelable {

    public final String name;
    public final String spotifyId;
    public final String thumbnailImageUrl;

    public ArtistDetails(Parcel input) {
        name = input.readString();
        spotifyId = input.readString();
        thumbnailImageUrl = input.readString();
    }

    public ArtistDetails(String name, String spotifyId, String thumbnailImageUrl) {
        this.name = name;
        this.spotifyId = spotifyId;
        this.thumbnailImageUrl = thumbnailImageUrl;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(spotifyId);
        dest.writeString(thumbnailImageUrl);
    }

    public static final Parcelable.Creator<ArtistDetails> CREATOR = new Parcelable.Creator<ArtistDetails>() {
        public ArtistDetails createFromParcel(Parcel in) {
            return new ArtistDetails(in);
        }

        public ArtistDetails[] newArray(int size) {
            return new ArtistDetails[size];
        }
    };

}
