/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import au.com.kbrsolutions.spotifystreamer.R;

/**
 * Retrieves artists data using search text entered by the user.
 */
public class TracksFragment extends ListFragment {
//    private TrackArrayAdapter<TrackDetails> mTrackArrayAdapter;
//    public final static String TRACKS_DATA = "tracks_data";

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

    @Override
    public void onResume() {
        super.onResume();
        if (getListAdapter().getCount() == 0) {
            setEmptyText(getActivity().getResources().getString(R.string.tracks_data_not_found));
        }
    }
}
