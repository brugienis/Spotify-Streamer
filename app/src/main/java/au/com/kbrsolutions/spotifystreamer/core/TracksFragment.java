/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import java.util.List;

/**
 * Retrieves artists data using search text entered by the user.
 */
public class TracksFragment extends ListFragment {

    private final static String LOG_TAG = TracksFragment.class.getSimpleName();


    public TracksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    /**
     * Shows top 10 tracks of the selected artist on the screen.
     */
    public void showTracksDetails(List<TrackDetails> trackDetails) {
        TrackArrayAdapter<TrackDetails> trackArrayAdapter = (TrackArrayAdapter) getListAdapter();
        trackArrayAdapter.clear();
        trackArrayAdapter.addAll(trackDetails);
    }

}