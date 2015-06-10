package au.com.kbrsolutions.spotifystreamer.core;

import android.app.Activity;
import android.app.ListFragment;

/**
 * Created by business on 10/06/2015.
 */
public class ArtistListFragment extends ListFragment {

    private ArtistsActivity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (ArtistsActivity) activity;
    }
}
