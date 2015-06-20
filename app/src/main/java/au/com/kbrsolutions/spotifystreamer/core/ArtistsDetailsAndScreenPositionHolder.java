package au.com.kbrsolutions.spotifystreamer.core;

import java.util.List;

/**
 * Created by business on 19/06/2015.
 */
public class ArtistsDetailsAndScreenPositionHolder {

    public final List<ArtistDetails> artistsDetailsList;
    public final int listViewFirstVisiblePosition;

    public ArtistsDetailsAndScreenPositionHolder(List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        this.artistsDetailsList = artistsDetailsList;
        this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
    }
}
