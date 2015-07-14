/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.fragments.PlayerControllerUi;

/**
 * Shows artists details or top 10 tracks of selected artist.
 */

public class SpotifyStreamerActivity extends ActionBarActivity
        implements ArtistsFragment.ArtistsFragmentCallbacks {

    private CharSequence mActivityTitle;
    private ArtistsFragment mArtistsFragment;
    private TracksFragment mTracksFragment;
    private boolean mTwoPane;
    private final String ACTIVITY_TITLE = "activity_title";
    private final String ACTIVITY_SUB_TITLE = "activity_sub_title";
    private final String ARTIST_TAG = "artist_tag";
    private final String TRACK_TAG = "track_tag";

    private final static String LOG_TAG = SpotifyStreamerActivity.class.getSimpleName();

    /**
     * Adds OnBackStackChangedListener. If there are no entries on the BackStack, user pressed
     * Back or Home Up button - show recent artists list.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "onCreate - start");
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
//                            .addToBackStack(TRACK_TAG)
                            .commit();
                }
            }
        } else {
            mTwoPane = false;
//            getSupportActionBar().setElevation(0f);
        }

        mArtistsFragment =
                (ArtistsFragment) getSupportFragmentManager().findFragmentByTag(ARTIST_TAG);

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
        mArtistsFragment.showArtistsDetails();
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
        Log.v(LOG_TAG, "onSaveInstanceState");

        outState.putCharSequence(ACTIVITY_TITLE, getSupportActionBar().getTitle());
        outState.putCharSequence(ACTIVITY_SUB_TITLE, getSupportActionBar().getSubtitle());
    }

    /**
     * Retrieves saved data. If search for artist's data is not in progress, show retrieved
     * on the screen.
     *
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.v(LOG_TAG, "onRestoreInstanceState");

        mActivityTitle = savedInstanceState.getCharSequence(ACTIVITY_TITLE);
        getSupportActionBar().setTitle(mActivityTitle);
        CharSequence activitySubtitle = savedInstanceState.getCharSequence(ACTIVITY_SUB_TITLE);
        getSupportActionBar().setSubtitle(activitySubtitle);

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            mArtistsFragment.showArtistsDetails();
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
        } else if (id == R.id.action_show_player) {
            showPlayer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPlayer() {
        PlayerControllerUi dialog = new PlayerControllerUi();   //.newInstance(tracks,position);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(android.R.id.content, dialog)
                .addToBackStack(null)
                .commit();
    }

    /**
     * On tablet - TracksFragment is visible - show empty list text.
     */
    @Override
    public void artistSearchStarted() {
        Log.v(LOG_TAG, "artistSearchStarted - mTracksFragment: " + mTracksFragment);
        if (mTracksFragment != null && mTracksFragment.isVisible()) {
            mTracksFragment.setListAdapter(new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>()));
            mTracksFragment.setEmptyText("No tracks available");
        }
    }

    /**
     * On tablet - TracksFragment is visible - show empty list text.
     */
    @Override
    public void artistSearchEnded() {
        Log.v(LOG_TAG, "showTracksData - mTracksFragment: " + mTracksFragment);
        if (mTracksFragment != null && mTracksFragment.isVisible()) {
            mTracksFragment.setEmptyText("Select an artist to see top 10 tracks");
        }
    }

    /**
     * Shows top 10 tracks of the artist selected on the artists screen.
     */
    @Override
    public void showTracksData(String artistName, List<TrackDetails> trackDetails) {
//        Log.v(LOG_TAG, "showTracksData - mTwoPane: " + mTwoPane);
//        getSupportActionBar().setTitle(mActivityTitle);
        getSupportActionBar().setSubtitle(artistName);
        mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
//        Log.v(LOG_TAG, "showTracksData - mTracksFragment: " + mTracksFragment);
        if (mTracksFragment == null) {
            mTracksFragment = new TracksFragment();
        }
        if (!mTwoPane) {
            mActivityTitle = getResources().getString(R.string.title_activity_tracks);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.left_dynamic_fragments_frame, mTracksFragment, TRACK_TAG)
                    .addToBackStack(TRACK_TAG)
                    .commit();
        }
        TrackArrayAdapter<TrackDetails> trackArrayAdapter = new TrackArrayAdapter<>(this, trackDetails);
        mTracksFragment.setListAdapter(trackArrayAdapter);
    }

}
