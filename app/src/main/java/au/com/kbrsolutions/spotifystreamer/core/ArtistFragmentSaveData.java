package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Created by business on 24/06/2015.
 */
public class ArtistFragmentSaveData {
    public final String artistName;
    public final int listViewFirstVisiblePosition;

    public ArtistFragmentSaveData(String artistName, int listViewFirstVisiblePosition) {
        this.artistName = artistName;
        this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
    }
}
