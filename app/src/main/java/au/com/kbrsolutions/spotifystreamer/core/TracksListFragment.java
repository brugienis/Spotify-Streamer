package au.com.kbrsolutions.spotifystreamer.core;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.kbrsolutions.spotifystreamer.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksListFragment extends Fragment {

    public TracksListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracks, container, false);
    }
}
