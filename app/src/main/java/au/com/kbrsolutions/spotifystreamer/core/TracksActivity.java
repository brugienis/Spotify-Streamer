package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class TracksActivity extends ActionBarActivity {

    private String message;
    private TracksListFragment mTracksListFragment;

    private final static String LOG_TAG = TracksActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        if (savedInstanceState == null) {
            if (mTracksListFragment == null) {
                mTracksListFragment = new TracksListFragment();
            }
            List<ArtistDetails> summaryItemsList = new ArrayList<ArtistDetails>();
            summaryItemsList.add(new ArtistDetails("name0", "id0", "thumbImage0"));
            summaryItemsList.add(new ArtistDetails("name1", "id1", "thumbImage1"));
            summaryItemsList.add(new ArtistDetails("name2", "id2", "thumbImage2"));

            TrackArrayAdapter artistArrayAdapter = new TrackArrayAdapter<ArtistDetails>(this, summaryItemsList);
            mTracksListFragment.setListAdapter(artistArrayAdapter);
            artistArrayAdapter.notifyDataSetChanged();
            getSupportFragmentManager().beginTransaction()
//            getFragmentManager().beginTransaction()
                    .add(R.id.fragments_frame, mTracksListFragment, "")
                    .commit();
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            message = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            mTracksListFragment.setMessage(message);
        }
        Log.v(LOG_TAG, "onCreate - message: " + message);

//        FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction;
//        getSupportFragmentManager().beginTransaction().replace(R.id.fragments_frame, mTracksListFragment).commit();
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

        return super.onOptionsItemSelected(item);
    }
}
