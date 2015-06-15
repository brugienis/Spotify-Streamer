package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Created by business on 13/06/2015.
 */
public class TrackDetails {

    public final String trackName;
    public final String albumName;
    public final String albumArtBigImageUrl;
    public final String albumArtSmallImageUrl;
    public final String previewUrl;

    public TrackDetails(String trackName, String albumName, String albumArtBigImageUrl, String albumArtSmallImageUrl, String previewUrl) {
        this.trackName = trackName;
        this.albumName = albumName;
        this.albumArtBigImageUrl = albumArtBigImageUrl;
        this.albumArtSmallImageUrl = albumArtSmallImageUrl;
        this.previewUrl = previewUrl;
    }

    @Override
    public String toString() {
        return "trackName: " + trackName + "; albumName: " + albumName + "; albumArtBigImageUrl: " + albumArtBigImageUrl + "; albumArtSmallImageUrl: " + albumArtSmallImageUrl + "; previewUrl: " + previewUrl;
    }
}
