package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import java.util.List;

public class TracksFragment extends ListFragment {

//    private TracksActivity mActivity;

    private final static String LOG_TAG = TracksFragment.class.getSimpleName();


    public TracksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        this.mActivity = (TracksActivity) activity;
//        Log.v(LOG_TAG, "mActivity: " + mActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    public void ShowTracksDetails(List<TrackDetails> trackDetails) {
        TrackArrayAdapter<TrackDetails> trackArrayAdapter = (TrackArrayAdapter) getListAdapter();
        trackArrayAdapter.clear();
        trackArrayAdapter.addAll(trackDetails);
    }

}
