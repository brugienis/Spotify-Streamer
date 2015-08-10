/*
 * Copyright (C) 2013 The Android Open Source Project
 */

package au.com.kbrsolutions.spotifystreamer.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.adapters.TrackArrayAdapter;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;
import au.com.kbrsolutions.spotifystreamer.fragments.ArtistsFragment;
import au.com.kbrsolutions.spotifystreamer.fragments.PlayerControllerUi;
import au.com.kbrsolutions.spotifystreamer.fragments.TracksFragment;
import au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService;
import au.com.kbrsolutions.spotifystreamer.utils.ProgressBarHandler;

/**
 * Shows artists details or top 10 tracks of selected artist.
 */

public class SpotifyStreamerActivity extends ActionBarActivity implements
        ArtistsFragment.ArtistsFragmentCallbacks,
        PlayerControllerUi.PlayerControllerUiCallbacks,
//        PlayerControllerUi.PlayerControllerUiCallbacks,
        TracksFragment.TracksFragmentCallbacks {
//        ServiceConnection {

    private CharSequence mActivityTitle;
    private ArtistsFragment mArtistsFragment;
    private TracksFragment mTracksFragment;
    private PlayerControllerUi mDialogFragment;
    private ProgressBarHandler mProgressBarHandler;
    private boolean isMusicPlayerServiceBounded;
    private boolean mTwoPane;
    private boolean showDialogFragment_AS_DIALOG_TEST_ONLY = true;
    // TODO: 10/08/2015 - I do not think I need that 
    private boolean mWasPlayerControllerUiVisible = false;
    private MusicPlayerService mMusicPlayerService;
    private final String ACTIVITY_TITLE = "activity_title";
    private final String ACTIVITY_SUB_TITLE = "activity_sub_title";
    private final String PLAYER_CONTROLLER_UI_VISIBLE = "player_controller_ui_visible";
    private final String ARTIST_TAG = "artist_tag";
    private final String TRACK_TAG = "track_tag";
    private final String PLAYER_TAG = "player_tag";

    private final static String LOG_TAG = SpotifyStreamerActivity.class.getSimpleName();

    /**
     * Adds OnBackStackChangedListener. If there are no entries on the BackStack, user pressed
     * Back or Home Up button - show recent artists list.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.v(LOG_TAG, "onCreate - start - activity hashCode: " + this.hashCode());
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


        mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
        if (findViewById(R.id.right_dynamic_fragments_frame) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
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

        mDialogFragment =
                (PlayerControllerUi) getSupportFragmentManager().findFragmentByTag(PLAYER_TAG);
        int bckStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
//        Log.v(LOG_TAG, "onCreate - BackStackEntryCount/mDialogFragment: " + bckStackEntryCount + "/" + mDialogFragment);

        /* count 1 - artist and tracks fragments: 2 - artists, tracks and player - DON'T add to BackStack */
        switch (bckStackEntryCount) {
            case 1:
                if (!mTwoPane) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.left_dynamic_fragments_frame, mTracksFragment, TRACK_TAG)
//                            .addToBackStack(TRACK_TAG)
                            .commit();
                }
                break;

            case 2:
//                Log.v(LOG_TAG, "onCreate - replacing with mDialogFragment");
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.left_dynamic_fragments_frame, mDialogFragment, PLAYER_TAG)
                        .commit();
                break;
        }

        mProgressBarHandler = new ProgressBarHandler(this);

//        Log.v(LOG_TAG, "onCreate - starting player service - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
        Intent intent = new Intent(getApplicationContext(), MusicPlayerService.class);
        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
//        Log.v(LOG_TAG, "onCreate - bind called             - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
        isMusicPlayerServiceBounded = true;
//        Log.v(LOG_TAG, "onCreate - end - BackStackEntryCount: " + bckStackEntryCount);
    }



    /**
     * Show recent artists data after user pressed Back or Up button.
     */
    private void showArtistsData() {
        mActivityTitle = getResources().getString(R.string.title_activity_artists);
        getSupportActionBar().setTitle(mActivityTitle);
        mArtistsFragment.showArtistsDetails();
    }

    public void showProgress() {
        mProgressBarHandler.show();
    }

    public void hideProgress() {
        mProgressBarHandler.hide();
    }

    /**
     * Up button was pressed - remove to top entry Back Stack
     */
    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        Log.v(LOG_TAG, "onSaveInstanceState");

        outState.putCharSequence(ACTIVITY_TITLE, getSupportActionBar().getTitle());
        outState.putCharSequence(ACTIVITY_SUB_TITLE, getSupportActionBar().getSubtitle());
        outState.putBoolean(PLAYER_CONTROLLER_UI_VISIBLE, mWasPlayerControllerUiVisible);
//        Log.v(LOG_TAG, "onSaveInstanceState - outState: " + outState);
    }

    /**
     * Retrieves saved data. If search for artist's data is not in progress, show retrieved
     * on the screen.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        Log.v(LOG_TAG, "onRestoreInstanceState");
//        Log.v(LOG_TAG, "onRestoreInstanceState - savedInstanceState: " + savedInstanceState);

        mActivityTitle = savedInstanceState.getCharSequence(ACTIVITY_TITLE);
        getSupportActionBar().setTitle(mActivityTitle);
        CharSequence activitySubtitle = savedInstanceState.getCharSequence(ACTIVITY_SUB_TITLE);
        getSupportActionBar().setSubtitle(activitySubtitle);
        mWasPlayerControllerUiVisible = savedInstanceState.getBoolean(PLAYER_CONTROLLER_UI_VISIBLE);

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            mArtistsFragment.showArtistsDetails();
        }
        Log.v(LOG_TAG, "onRestoreInstanceState - end - mWasPlayerControllerUiVisible/mDialogFragment: " + mWasPlayerControllerUiVisible + "/" + mDialogFragment);
//        if (mWasPlayerControllerUiVisible && mDialogFragment != null) {
//            mDialogFragment.setReconnectToPlayerService();
//        }
    }

    public boolean wasPlayerControllerUiVisibleOnRestart() {
//        boolean wasPlayerControllerUiVisible = mWasPlayerControllerUiVisible;
//        mWasPlayerControllerUiVisible = false;
        return mWasPlayerControllerUiVisible;
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
            // TODO: 15/07/2015 remove this menu after testing 
            showPlayerController(-1, false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * On tablet - TracksFragment is visible - show empty list text.
     */
    @Override
    public void artistSearchStarted() {
//        Log.v(LOG_TAG, "artistSearchStarted - mTracksFragment: " + mTracksFragment);
        showProgress();
        if (mTracksFragment != null && mTracksFragment.isVisible()) {
            mTracksFragment.setListAdapter(new TrackArrayAdapter<>(this, new ArrayList<TrackDetails>()));
            mTracksFragment.setEmptyText("No tracks available");
        }
    }

    /**
     * On tablet - TracksFragment is visible - show text to select artist.
     */
    @Override
    public void artistSearchEnded() {
//        Log.v(LOG_TAG, "showTracksData - mTracksFragment: " + mTracksFragment);
        if (mTracksFragment != null && mTracksFragment.isVisible()) {
            mTracksFragment.setEmptyText("Select an artist to see top 10 tracks");
        }
    }

    /**
     * Shows top 10 tracks of the artist selected on the artists screen.
     */
    @Override
    public void showTracksData(String artistName, List<TrackDetails> tracksDetails) {
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
        TrackArrayAdapter<TrackDetails> trackArrayAdapter = new TrackArrayAdapter<>(this, tracksDetails);
        mTracksFragment.setListAdapter(trackArrayAdapter);
    }

    @Override
    public void newTrackClicked(int selectedTrack) {
//        Log.v(LOG_TAG, "newTrackClicked - start");
        showPlayerController(selectedTrack, false);
    }
    @Override
    public void showPlayerUiAndReconnectTuPlayerService() {
        showPlayerController(-1, true);
    }

    private void showPlayerController(int selectedTrack, boolean reconnectToPlayerService) {
        // FIXME: 10/08/2015 do I need to create a ne instance - check if the previous one still exists
        mDialogFragment = PlayerControllerUi.newInstance(
                mArtistsFragment.getArtistName(),
                (ArrayList<TrackDetails>) mTracksFragment.getTrackDetails(),
                selectedTrack,
                reconnectToPlayerService);
//        mDialogFragment.setCancelable(false);
//        if (mTwoPane) {
        mWasPlayerControllerUiVisible = true;
        if (showDialogFragment_AS_DIALOG_TEST_ONLY) {
            mDialogFragment.show(getSupportFragmentManager(), PLAYER_TAG);
        } else {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.left_dynamic_fragments_frame, mDialogFragment, PLAYER_TAG)
                    .addToBackStack(null)
                    .commit();
        }
        Log.v(LOG_TAG, "showPlayerController - BackStackEntryCount/mWasPlayerControllerUiVisible: " + getSupportFragmentManager().getBackStackEntryCount() + "/" + mWasPlayerControllerUiVisible);
    }

    @Override
    public void playerControllerUiIdNotVisible() {
        mWasPlayerControllerUiVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
//        Log.i(LOG_TAG, "onStop - start- isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
        // Unbind from the service
        if (isMusicPlayerServiceBounded) {
            mMusicPlayerService.processBeforeDisconnectingFromService(false);
            unbindService(mServiceConnection);
            isMusicPlayerServiceBounded = false;
//            Log.i(LOG_TAG, "onStop - end - unbindService called");
        }
//        Log.i(LOG_TAG, "onStop - end - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
    }

    private void processAfterConnectedToService() {
        mMusicPlayerService.processAfterConnectedToService(false);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            Log.i(LOG_TAG, "onServiceConnected - start- isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
            mMusicPlayerService = ((MusicPlayerService.LocalBinder) service).getService();
            isMusicPlayerServiceBounded = true;
            processAfterConnectedToService();
//            Log.i(LOG_TAG, "onServiceConnected - end - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LOG_TAG, "onServiceDisconnected - start");
            // Unbind from the service
//            if (isMusicPlayerServiceBounded) {
//                Log.i(LOG_TAG, "onServiceDisconnected - calling unBind");
//                unbindService(mServiceConnection);
//                isMusicPlayerServiceBounded = false;
//            }
            mMusicPlayerService = null;
            isMusicPlayerServiceBounded = false;
        }
    };
}
