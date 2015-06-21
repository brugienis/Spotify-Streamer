package au.com.kbrsolutions.spotifystreamer.core;

import java.util.List;

/**
 * Created by business on 19/06/2015.
 */
public class SaveRestoreArtistDetailsHolder {

    public final CharSequence artistName;
    public final List<ArtistDetails> artistsDetailsList;
    public final int listViewFirstVisiblePosition;

    public SaveRestoreArtistDetailsHolder(CharSequence artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        this.artistName = artistName;
        this.artistsDetailsList = artistsDetailsList;
        this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
    }
}
