/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

/**
 * Contains ArtistsFragment data that need to be saved in parent activity's
 * onSaveInstanceState method.
 */
public class ArtistFragmentSaveData {
    public final String artistName;
    public final int listViewFirstVisiblePosition;

    public ArtistFragmentSaveData(String artistName, int listViewFirstVisiblePosition) {
        this.artistName = artistName;
        this.listViewFirstVisiblePosition = listViewFirstVisiblePosition;
    }
}
