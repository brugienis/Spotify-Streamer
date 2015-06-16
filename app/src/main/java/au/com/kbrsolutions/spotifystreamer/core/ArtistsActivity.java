package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

    private ArtistsListFragment mArtistsListFragment;
    private ArtistArrayAdapter artistArrayAdapter;
    private TextView sarchText;
    private InputMethodManager imm;

    private final static String LOG_TAG = ArtistsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        sarchText = (TextView) findViewById(R.id.searchTextView);
        sarchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleSearchButtonClicked(actionId);
            }
        });
        if (savedInstanceState == null) {
            if (mArtistsListFragment == null) {
                mArtistsListFragment = new ArtistsListFragment();
//                Log.v(LOG_TAG, "created TracksListFragment");
            }
            List<ArtistDetails> tracksItemsList = new ArrayList<>();
//            tracksItemsList.add(new TrackDetails("track0", "id0", "thumbImage0", null, null));
//            tracksItemsList.add(new TrackDetails("track1", "id1", "thumbImage1", null, null));
//            tracksItemsList.add(new TrackDetails("track2", "id2", "thumbImage2", null, null));

            ArtistArrayAdapter<TrackDetails> trackArrayAdapter = new ArtistArrayAdapter<>(this, tracksItemsList);
            mArtistsListFragment.setListAdapter(trackArrayAdapter);
//            mArtistsListFragment.setEmptyText("Enter artist name"); // got java.lang.IllegalStateException: Content view not yet created
            trackArrayAdapter.notifyDataSetChanged();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragments_frame, mArtistsListFragment, "")
                    .commit();
        }
    }

    private boolean handleSearchButtonClicked(int actionId) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String artistName = sarchText.getText().toString();
            if (artistName.trim().length() > 0 && mArtistsListFragment != null) {
                mArtistsListFragment.sendArtistsDataRequestToSpotify(artistName);
                handled = true;
                hideKeyboard();
            }
        }
//        Log.v(LOG_TAG, "handleSearchButtonClicked");
        return handled;
    }

    private void hideKeyboard() {
        if (sarchText != null && sarchText.getWindowToken() != null && imm != null) {
            imm.hideSoftInputFromWindow(sarchText.getWindowToken(), 0);
        }
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
