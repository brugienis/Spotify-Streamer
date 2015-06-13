package au.com.kbrsolutions.spotifystreamer.core;

import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

//

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksListFragment extends ListFragment {
//public class TracksListFragment extends ListFragment {

    private String mArtistId;

    private final static String LOG_TAG = TracksListFragment.class.getSimpleName();

    public TracksListFragment() {
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_tracks, container, false);
//    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public void setMessage(String message) {
        this.mArtistId = message;
        Log.v(LOG_TAG, "setMessage - got message: " + message);
    }
}
