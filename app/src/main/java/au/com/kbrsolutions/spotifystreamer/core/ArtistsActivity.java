package au.com.kbrsolutions.spotifystreamer.core;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class ArtistsActivity extends ActionBarActivity  implements ArtistsFragment.ArtistsFragmentCallbacks {

    private ArtistsFragment mArtistsFragment;
    private final String ARTIST_NAME = "artist_name";
    private final String ARTIST_TAG = "artist_tag";
    private final String ARTISTS_DATA = "artists_data";
    private final String LIST_VIEW_FIRST_VISIBLE_POSITION = "listViewFirstVisiblePosition";

    private final static String LOG_TAG = ArtistsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists);

        mArtistsFragment = (ArtistsFragment) getSupportFragmentManager().findFragmentByTag(ARTIST_TAG);

        Log.v(LOG_TAG, "onCreate - mArtistsFragment: " + mArtistsFragment);
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mArtistsFragment == null) {
            mArtistsFragment = new ArtistsFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragments_frame, mArtistsFragment, ARTIST_TAG).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SaveRestoreArtistDetailsHolder saveRestoreArtistDetailsHolder = mArtistsFragment.getArtistsDetails();
        outState.putCharSequence(ARTIST_NAME, saveRestoreArtistDetailsHolder.artistName);
        List<ArtistDetails> artistDetailsList = saveRestoreArtistDetailsHolder.artistsDetailsList;
        if (artistDetailsList != null && artistDetailsList.size() > 0) {
            outState.putParcelableArrayList(ARTISTS_DATA, (ArrayList)artistDetailsList);
            outState.putInt(LIST_VIEW_FIRST_VISIBLE_POSITION, saveRestoreArtistDetailsHolder.listViewFirstVisiblePosition);
            Log.v(LOG_TAG, "onSaveInstanceState - done - saved: " + artistDetailsList.size());
        }
        Log.v(LOG_TAG, "onSaveInstanceState - done");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<ArtistDetails> artistDetailsList = savedInstanceState.getParcelableArrayList(ARTISTS_DATA);
        int listViewFirstVisiblePosition = savedInstanceState.getInt(LIST_VIEW_FIRST_VISIBLE_POSITION);
        if (!mArtistsFragment.isSearchInProgress()) {
            mArtistsFragment.showRestoredArtistsDetails(
                    savedInstanceState.getCharSequence(ARTIST_NAME),
                    artistDetailsList,
                    listViewFirstVisiblePosition);
        }
        if (artistDetailsList != null) {
            Log.v(LOG_TAG, "onRestoreInstanceState - done - artistDetailsList: " + artistDetailsList.size());
        }
        Log.v(LOG_TAG, "onRestoreInstanceState - done");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artists_activity_new, menu);
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

    @Override
    public void onPostExecute() {
        Log.v(LOG_TAG, "onPostExecute - start");
        mArtistsFragment.showArtistsDetails(0);
        Log.v(LOG_TAG, "onPostExecute - end");
    }
}
