package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.R;

public class TracksActivity extends ActionBarActivity {     //} implements TracksListFragment.TracksListFragmentCallbacks {

//    private String message;
    private TracksListFragment mTracksListFragment;
    private final String TRACK_TAG = "track_tag";

    private final static String LOG_TAG = TracksActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);


        mTracksListFragment = (TracksListFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
        Log.v(LOG_TAG, "onCreate - mArtistsFragment: " + mTracksListFragment);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTracksListFragment == null) {
            mTracksListFragment = new TracksListFragment();
            TrackArrayAdapter<TrackDetails> trackArrayAdapter = new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>());
            mTracksListFragment.setListAdapter(trackArrayAdapter);
            getSupportFragmentManager().beginTransaction().add(R.id.fragments_frame, mTracksListFragment, TRACK_TAG).commit();
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String message = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            mTracksListFragment.sendArtistsDataRequestToSpotify(message);
        }
    }

    void goBack() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        Log.v(LOG_TAG, "onOptionsItemSelected  - id: " + id);
        return super.onOptionsItemSelected(item);
//        finish();
//        return true;
    }

}
