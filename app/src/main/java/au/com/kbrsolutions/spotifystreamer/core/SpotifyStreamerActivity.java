package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class SpotifyStreamerActivity extends ActionBarActivity  implements ArtistsFragment.ArtistsFragmentCallbacks {

    private ArtistsFragment mArtistsFragment;
    private TracksFragment mTracksFragment;
    private CharSequence artistName;
    private List<ArtistDetails> artistsDetailsList;
    private int artistsListViewFirstVisiblePosition;
    private final String ARTIST_NAME = "artist_name";
    private final String ARTIST_TAG = "artist_tag";
    private final String TRACK_TAG = "track_tag";
    private final String ARTISTS_DATA = "artists_data";
    private final String LIST_VIEW_FIRST_VISIBLE_POSITION = "listViewFirstVisiblePosition";

    private final static String LOG_TAG = SpotifyStreamerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotifystreamer);

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        // Update your UI here.
                        shouldDisplayHomeUp();
                        int cnt = getSupportFragmentManager().getBackStackEntryCount();
//                        Log.v(LOG_TAG, "onBackStackChanged - called - cnt: " + cnt);
                        if (cnt == 0) {      /* we came back from artist's tracks to artists list */
                            showPrevArtistsData();
                        }
                    }
                });
        shouldDisplayHomeUp();

        mArtistsFragment = (ArtistsFragment) getSupportFragmentManager().findFragmentByTag(ARTIST_TAG);

//        Log.v(LOG_TAG, "onCreate - mArtistsFragment: " + mArtistsFragment);
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mArtistsFragment == null) {
            mArtistsFragment = new ArtistsFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragments_frame, mArtistsFragment, ARTIST_TAG).commit();
        }
    }

    @Override
    public void showTracks(int listViewFirstVisiblePosition, List<TrackDetails> trackDetails) {
//        Log.v(LOG_TAG, "showTracks - start - tracks: " + trackDetails.size());
        getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_tracks));
        this.artistsListViewFirstVisiblePosition = listViewFirstVisiblePosition;
        mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
        if (mTracksFragment == null) {
            mTracksFragment = new TracksFragment();
            TrackArrayAdapter<TrackDetails> trackArrayAdapter = new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>());
            mTracksFragment.setListAdapter(trackArrayAdapter);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragments_frame, mTracksFragment, TRACK_TAG).addToBackStack(TRACK_TAG).commit();
        }
        mTracksFragment.ShowTracksDetails(trackDetails);
    }

    private void showPrevArtistsData() {
        getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_artists));
        mArtistsFragment.showRestoredArtistsDetails(artistName, artistsDetailsList, artistsListViewFirstVisiblePosition);
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = getSupportFragmentManager().getBackStackEntryCount()>0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(ARTIST_NAME, artistName);
        List<ArtistDetails> artistDetailsList = artistsDetailsList;
        if (artistDetailsList != null && artistDetailsList.size() > 0) {
            outState.putParcelableArrayList(ARTISTS_DATA, (ArrayList)artistDetailsList);
            outState.putInt(LIST_VIEW_FIRST_VISIBLE_POSITION, mArtistsFragment.getListViewFirstVisiblePosition());
//            Log.v(LOG_TAG, "onSaveInstanceState - done - saved: " + artistDetailsList.size());
        }
//        Log.v(LOG_TAG, "onSaveInstanceState - done");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        artistName = savedInstanceState.getCharSequence(ARTIST_NAME);
        artistsDetailsList = savedInstanceState.getParcelableArrayList(ARTISTS_DATA);
        artistsListViewFirstVisiblePosition = savedInstanceState.getInt(LIST_VIEW_FIRST_VISIBLE_POSITION);
        if (!mArtistsFragment.isSearchInProgress()) {
            mArtistsFragment.showRestoredArtistsDetails(
                    savedInstanceState.getCharSequence(ARTIST_NAME),
                    artistsDetailsList,
                    artistsListViewFirstVisiblePosition);
        }
        if (artistsDetailsList != null) {
//            Log.v(LOG_TAG, "onRestoreInstanceState - done - artistDetailsList: " + artistsDetailsList.size());
        }
//        Log.v(LOG_TAG, "onRestoreInstanceState - done");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artists_fragment, menu);
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPostExecute(CharSequence artistName, List<ArtistDetails> artistsDetailsList, int listViewFirstVisiblePosition) {
//        Log.v(LOG_TAG, "onPostExecute - start");
        this.artistsDetailsList = artistsDetailsList;
        this.artistName = artistName;
        this.artistsListViewFirstVisiblePosition = listViewFirstVisiblePosition;
        mArtistsFragment.showRestoredArtistsDetails(artistName, artistsDetailsList, artistsListViewFirstVisiblePosition);
//        mArtistsFragment.showArtistsDetails(0);
//        Log.v(LOG_TAG, "onPostExecute - end");
    }
}
