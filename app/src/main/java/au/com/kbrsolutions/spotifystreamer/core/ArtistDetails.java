package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Created by business on 9/06/2015.
 */
class ArtistDetails {

    final String name;
    final String spotifyId;
    final String thumbnailImageUrl;

    ArtistDetails(String name, String spotifyId, String thumbnailImageUrl) {
        this.name = name;
        this.spotifyId = spotifyId;
        this.thumbnailImageUrl = thumbnailImageUrl;

    }

}
