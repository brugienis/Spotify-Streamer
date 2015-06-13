package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Created by business on 9/06/2015.
 */
public class ArtistDetails {

    public final String name;
    public final String spotifyId;
    public final String thumbnailImageUrl;

    public ArtistDetails(String name, String spotifyId, String thumbnailImageUrl) {
        this.name = name;
        this.spotifyId = spotifyId;
        this.thumbnailImageUrl = thumbnailImageUrl;

    }

}
