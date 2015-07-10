/*
 * Copyright (C) 2013 The Android Open Source Project
 */

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

/**
 * Shows artists details or top 10 tracks of selected artist.
 */

public class SpotifyStreamerActivity extends ActionBarActivity
        implements ArtistsFragment.ArtistsFragmentCallbacks {

    private CharSequence mActivityTitle;
    private ArtistsFragment mArtistsFragment;
    private TracksFragment mTracksFragment;
    private String mArtistName;
    private boolean mTwoPane;
    private List<ArtistDetails> mArtistsDetailsList;
    private int mArtistsListViewFirstVisiblePosition;
    private final String ACTIVITY_TITLE = "activity_title";
    private final String ARTIST_NAME = "artist_name";
    private final String ARTIST_TAG = "artist_tag";
    private final String TRACK_TAG = "track_tag";
    private final String ARTISTS_DATA = "artists_data";
    private final String LIST_VIEW_FIRST_VISIBLE_POSITION = "list_view_first_visible_position";

    private final static String LOG_TAG = SpotifyStreamerActivity.class.getSimpleName();

    /**
     * Adds OnBackStackChangedListener. If there are no entries on the BackStack, user pressed
     * Back or Home Up button - show recent artists list.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotifystreamer);

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        int cnt = getSupportFragmentManager().getBackStackEntryCount();
                        if (cnt == 0) {      /* we came back from artist's tracks to artists list */
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            showArtistsData();
                        } else {
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                });

        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager()
                .getBackStackEntryCount() > 0);


        if (findViewById(R.id.right_dynamic_fragments_frame) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {


                mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
                if (mTracksFragment == null) {
                    mTracksFragment = new TracksFragment();
                    TrackArrayAdapter<TrackDetails> trackArrayAdapter =
                            new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>());
                    mTracksFragment.setListAdapter(trackArrayAdapter);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.right_dynamic_fragments_frame, mTracksFragment, TRACK_TAG)
                            .addToBackStack(TRACK_TAG)
                            .commit();
                }
//                mTracksFragment.showTracksDetails(trackDetails);
            }
        } else {
            mTwoPane = false;
//            getSupportActionBar().setElevation(0f);
        }

        mArtistsFragment =
                (ArtistsFragment) getSupportFragmentManager()
                        .findFragmentByTag(ARTIST_TAG);

        if (mArtistsFragment == null) {
            mArtistsFragment = new ArtistsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.left_dynamic_fragments_frame, mArtistsFragment, ARTIST_TAG)
                    .commit();
        }
    }

    /**
     * Show recent artists data after user pressed Back or Up button.
     */
    private void showArtistsData() {
        mActivityTitle = getResources().getString(R.string.title_activity_artists);
        getSupportActionBar().setTitle(mActivityTitle);
        mArtistsFragment.showArtistsDetails(mArtistName, mArtistsDetailsList,
                mArtistsListViewFirstVisiblePosition);
    }

    /**
     * Up button was pressed - remove to top entry Back Stack
     */
    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(ACTIVITY_TITLE, getSupportActionBar().getTitle());
        ArtistFragmentSaveData artistFragmentSaveData = mArtistsFragment.getDataToSave();
        outState.putString(ARTIST_NAME, artistFragmentSaveData.artistName);
//        List<ArtistDetails> artistDetailsList = mArtistsDetailsList;
        List<ArtistDetails> artistDetailsList = artistFragmentSaveData.mArtistsDetailsList;
        if (artistDetailsList != null && artistDetailsList.size() > 0) {
            outState.putParcelableArrayList(ARTISTS_DATA, (ArrayList)artistDetailsList);
            outState.putInt(LIST_VIEW_FIRST_VISIBLE_POSITION,
                    artistFragmentSaveData.listViewFirstVisiblePosition);
        }
    }

    /**
     * Retrieves saved data. If search for artist's data is not in progress, show retrieved
     * on the screen.
     *
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mActivityTitle = savedInstanceState.getCharSequence(ACTIVITY_TITLE);
        getSupportActionBar().setTitle(mActivityTitle);
        mArtistName = savedInstanceState.getString(ARTIST_NAME);
        mArtistsDetailsList = savedInstanceState.getParcelableArrayList(ARTISTS_DATA);
        mArtistsListViewFirstVisiblePosition =
                savedInstanceState.getInt(LIST_VIEW_FIRST_VISIBLE_POSITION);
        if (!mArtistsFragment.isSearchInProgress()) {
            mArtistsFragment.showArtistsDetails(
                    savedInstanceState.getCharSequence(ARTIST_NAME),
                    mArtistsDetailsList,
                    mArtistsListViewFirstVisiblePosition);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spotifystreamer_activity, menu);
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

    /**
     * Shows top 10 tracks of the artist selected on the artists screen.
     */
    @Override
    public void showTracksData(int listViewFirstVisiblePosition, List<TrackDetails> trackDetails) {
        mActivityTitle = getResources().getString(R.string.title_activity_tracks);
        getSupportActionBar().setTitle(mActivityTitle);
        this.mArtistsListViewFirstVisiblePosition = listViewFirstVisiblePosition;
        mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
        if (mTracksFragment == null) {
            mTracksFragment = new TracksFragment();
            TrackArrayAdapter<TrackDetails> trackArrayAdapter =
                    new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>());
            mTracksFragment.setListAdapter(trackArrayAdapter);
            int frameLayout = mTwoPane ? R.id.right_dynamic_fragments_frame : R.id.left_dynamic_fragments_frame;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(frameLayout, mTracksFragment, TRACK_TAG)
                    .addToBackStack(TRACK_TAG)
                    .commit();
        }
        mTracksFragment.showTracksDetails(trackDetails);
    }

}
