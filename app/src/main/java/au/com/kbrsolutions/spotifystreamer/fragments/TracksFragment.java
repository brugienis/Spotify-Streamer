/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.adapters.TrackArrayAdapter;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Retrieves artists data using search text entered by the user.
 */
public class TracksFragment extends ListFragment {

    /**
     * Declares callback methods that have to be implemented by parent Activity
     */
    public interface TracksFragmentCallbacks {
        void newTrackClicked(int selectedTrack);
    }

    private TracksFragmentCallbacks mCallbacks;

    private final static String LOG_TAG = TracksFragment.class.getSimpleName();

    public TracksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        try {
            mCallbacks = (TracksFragmentCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException(
                    getActivity().getResources()
                            .getString(R.string.callbacks_not_implemented, activity.toString()));
        }
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ArrayList<TrackDetails> tracksDetails = ((TrackArrayAdapter) getListAdapter()).getTracksDetails();
        mCallbacks.newTrackClicked(position);
//        TrackDetails  trackDetails = (TrackDetails) getListAdapter().getItem(position);
//        Log.v(LOG_TAG, "onListItemClick - " + trackDetails.trackName);
//        if (mActivity.isNotConnectedToGoogleDrive(LOG_TAG + "onListItemClick")) {
//            return;
//        }
//        mActivity.fileOrFolderClicked(position);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getListAdapter().getCount() == 0) {
            setEmptyText(getActivity().getResources().getString(R.string.tracks_data_not_found));
        }
    }
}
