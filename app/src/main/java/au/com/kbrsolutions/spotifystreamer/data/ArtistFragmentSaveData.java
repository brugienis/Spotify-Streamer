/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.data;

import java.util.List;

/**
 * Contains ArtistsFragment data that need to be saved in parent activity's
 * onSaveInstanceState method.
 */
public class ArtistFragmentSaveData {
    public final String artistName;
    public final List<ArtistDetails> mArtistsDetailsList;
    public final int listViewFirstVisiblePosition;

    public ArtistFragmentSaveData(String artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
        this.artistName = artistName;
        this.mArtistsDetailsList = artistsDetailsList;
        this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
    }
}
