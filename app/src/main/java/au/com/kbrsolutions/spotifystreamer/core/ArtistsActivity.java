package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;

public class ArtistsActivity extends ActionBarActivity {

    private ArtistsListFragment mArtistsFragment;
    private TextView sarchText;
    private InputMethodManager imm;
    private final String ARTIST_TAG = "artistTag";
    private final String LIST_VIEW_FIRST_VISIBLE_POSITION = "listViewFirstVisiblePosition";

    private final static String LOG_TAG = ArtistsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "onCreate - start - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.activity_artists);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        sarchText = (TextView) findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });
        if (mArtistsFragment == null) {
            mArtistsFragment = new ArtistsListFragment();
        }
        List<ArtistDetails> tracksItemsList = new ArrayList<>();
//            tracksItemsList.add(new TrackDetails("track0", "id0", "thumbImage0", null, null));
//            tracksItemsList.add(new TrackDetails("track1", "id1", "thumbImage1", null, null));
//            tracksItemsList.add(new TrackDetails("track2", "id2", "thumbImage2", null, null));

        ArtistArrayAdapter<TrackDetails> trackArrayAdapter = new ArtistArrayAdapter<>(this, tracksItemsList);
        mArtistsFragment.setListAdapter(trackArrayAdapter);
//        trackArrayAdapter.notifyDataSetChanged();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragments_frame, mArtistsFragment, ARTIST_TAG)
                .commit();
    }

    private boolean handleSearchButtonClicked(int actionId) {
        Log.v(LOG_TAG, "handleSearchButtonClicked - start");
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = sarchText.getText().toString();
            if (artistName.trim().length() > 0 && mArtistsFragment != null) {
                mArtistsFragment.sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            }
        }
        Log.v(LOG_TAG, "handleSearchButtonClicked - end");
        return handled;
    }

    private void hideKeyboard() {
        if (sarchText != null && sarchText.getWindowToken() != null && imm != null) {
            imm.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
        }
    }

    private final String ARTISTS_DATA = "artistsData";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//
        ArtistsDetailsAndScreenPositionHolder artistsDetailsAndScreenPositionHolder = mArtistsFragment.getArtistsDetails();
        List<ArtistDetails> artistDetailsList = artistsDetailsAndScreenPositionHolder.artistsDetailsList;
        int listViewFirstVisiblePosition = artistsDetailsAndScreenPositionHolder.listViewFirstVisiblePosition;
        if (artistDetailsList != null && artistDetailsList.size() > 0) {
            outState.putParcelableArrayList(ARTISTS_DATA, (ArrayList)artistDetailsList);
            outState.putInt(LIST_VIEW_FIRST_VISIBLE_POSITION, artistsDetailsAndScreenPositionHolder.listViewFirstVisiblePosition);
            Log.v(LOG_TAG, "onSaveInstanceState - done - saved: " + artistDetailsList.size());
        }
        Log.v(LOG_TAG, "onSaveInstanceState - done");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<ArtistDetails> artistDetailsList = savedInstanceState.getParcelableArrayList(ARTISTS_DATA);
        if (artistDetailsList != null) {
            int listViewFirstVisiblePosition = savedInstanceState.getInt(LIST_VIEW_FIRST_VISIBLE_POSITION);
            mArtistsFragment.showArtistsDetails(artistDetailsList, listViewFirstVisiblePosition);
            Log.v(LOG_TAG, "onRestoreInstanceState - done - artistDetailsList: " + artistDetailsList.size());
        }
        Log.v(LOG_TAG, "onRestoreInstanceState - done");
    }

    @Override
    protected void onPause() {

        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        // from: http://stackoverflow.com/questions/8122625/getextractedtext-on-inactive-inputconnection-warning-on-android

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artists, menu);
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
