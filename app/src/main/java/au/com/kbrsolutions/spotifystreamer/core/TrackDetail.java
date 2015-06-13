package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Created by business on 13/06/2015.
 */
public class TrackDetail {

    public final String trackName;
    public final String albumName;
    public final String albumThumbnailImageUrl;
    public final String previewUrl;

    public TrackDetail(String trackName, String albumName, String thumbnailImageUrl, String previewUrl) {
        this.trackName = trackName;
        this.albumName = albumName;
        this.albumThumbnailImageUrl = thumbnailImageUrl;
        this.previewUrl = previewUrl;

    }
}
