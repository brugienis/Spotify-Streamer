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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.adapters.TrackArrayAdapter;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;
import au.com.kbrsolutions.spotifystreamer.events.MusicPlayerServiceEvents;
import au.com.kbrsolutions.spotifystreamer.events.SpotifyStreamerActivityEvents;
import au.com.kbrsolutions.spotifystreamer.fragments.ArtistsFragment;
import au.com.kbrsolutions.spotifystreamer.fragments.PlayerControllerUi;
import au.com.kbrsolutions.spotifystreamer.fragments.TracksFragment;
import au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService;
import au.com.kbrsolutions.spotifystreamer.utils.ProgressBarHandler;
import de.greenrobot.event.EventBus;

/**
 * Shows artists details or top 10 tracks of selected artist.
 */

public class SpotifyStreamerActivity extends AppCompatActivity implements
        ArtistsFragment.ArtistsFragmentCallbacks,
        PlayerControllerUi.PlayerControllerUiCallbacks,
        TracksFragment.TracksFragmentCallbacks {

    private Button playerStatusBt;
    private TextView artistTv;
    private TextView albumTv;
    private TextView currTrackNameTv;
    private CharSequence mActivityTitle;
    private CharSequence mActivitySubtitle;
    private ArtistsFragment mArtistsFragment;
    private TracksFragment mTracksFragment;
    private PlayerControllerUi mDialogFragment;
    private ProgressBarHandler mProgressBarHandler;
    private boolean isMusicPlayerServiceBounded;
    private boolean mTwoPane;
    private boolean mWasPlayNowVisible;
//    private boolean showDialogFragment_AS_DIALOG_TEST_ONLY = false;
    private boolean mWasPlayerControllerUiVisible = false;
    private MusicPlayerService mMusicPlayerService;
    private EventBus eventBus;

    private final String ACTIVITY_TITLE = "activity_title";
    private final String ACTIVITY_SUB_TITLE = "activity_sub_title";
    private final String PLAY_NOW_VISIBLE = "play_now_visible";
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
        setContentView(R.layout.activity_spotifystreamer);
        if (eventBus == null) {
            eventBus = EventBus.getDefault();
            eventBus.register(this);
        }
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        int cnt = getSupportFragmentManager().getBackStackEntryCount();
                        if (cnt == 0) {      /* we came back from artist's tracks to artists list */
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            showArtistsData();
                        } else if (!mTwoPane) {
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                });

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
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.left_dynamic_fragments_frame, mDialogFragment, PLAYER_TAG)
                        .commit();
                break;
        }

        mProgressBarHandler = new ProgressBarHandler(this);
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
     * Make progress bar visible
     */
    public void showProgress() {
        mProgressBarHandler.show();
    }


    /**
     * Make progress bar invisible
     */
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

        outState.putCharSequence(ACTIVITY_TITLE, mActivityTitle);
        outState.putCharSequence(ACTIVITY_SUB_TITLE, mActivitySubtitle);
        outState.putBoolean(PLAYER_CONTROLLER_UI_VISIBLE, mWasPlayerControllerUiVisible);
        outState.putBoolean(PLAY_NOW_VISIBLE, mWasPlayNowVisible);
    }

    /**
     * Retrieves saved data. If search for artist's data is not in progress, show retrieved
     * on the screen.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mActivityTitle = savedInstanceState.getCharSequence(ACTIVITY_TITLE);
        getSupportActionBar().setTitle(mActivityTitle);
        mActivitySubtitle = savedInstanceState.getCharSequence(ACTIVITY_SUB_TITLE);
        getSupportActionBar().setSubtitle(mActivitySubtitle);
        mWasPlayerControllerUiVisible = savedInstanceState.getBoolean(PLAYER_CONTROLLER_UI_VISIBLE);
        mWasPlayNowVisible = savedInstanceState.getBoolean(PLAY_NOW_VISIBLE);

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            mArtistsFragment.showArtistsDetails();
        }
    }

    /**
     * Returns true if Player UI was visible on app restart
     */
    @Override
    public boolean wasPlayerControllerUiVisibleOnRestart() {
        return mWasPlayerControllerUiVisible;
    }

    /**
     * Set value of mWasPlayerControllerUiVisible to indicate Player UI visibility
     */
    @Override
    public void setWasPlayerControllerUiVisible(boolean value) {
        mWasPlayerControllerUiVisible = value;
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

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * On tablet - TracksFragment is visible - show empty list text.
     */
    @Override
    public void artistSearchStarted() {
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
        if (mTracksFragment != null && mTracksFragment.isVisible()) {
            mTracksFragment.setEmptyText("Select an artist to see top 10 tracks");
        }
    }

    /**
     * Shows top 10 tracks of the artist selected on the artists screen.
     */
    @Override
    public void showTracksData(String artistName, List<TrackDetails> tracksDetails) {
        mActivitySubtitle = artistName;
        getSupportActionBar().setSubtitle(artistName);
        mTracksFragment = (TracksFragment) getSupportFragmentManager().findFragmentByTag(TRACK_TAG);
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

    /**
     * User clicked on a track - show Player UI.
     */
    @Override
    public void newTrackClicked(int selectedTrack) {
        showPlayerController(selectedTrack, false);
    }

    /**
     * Show P{layer UI and reconnect to the Music Player service.
     */
    @Override
    public void showPlayerUiAndReconnectTuPlayerService() {
        showPlayerController(-1, true);
    }

    private void showPlayerController(int selectedTrack, boolean reconnectToPlayerService) {
        PlayerControllerUi mDialogFragmentNow =
                (PlayerControllerUi) getSupportFragmentManager().findFragmentByTag(PLAYER_TAG);
        if (mDialogFragmentNow != null) {
            if (reconnectToPlayerService) {
                mDialogFragmentNow.setReconnectToPlayerService();
            }
            return;
        }
        mDialogFragment = PlayerControllerUi.newInstance(
                mArtistsFragment.getSelectedArtistName(),
                mTracksFragment.getTrackDetails(),
                selectedTrack,
                reconnectToPlayerService);

        if (mTwoPane) {
//        if (showDialogFragment_AS_DIALOG_TEST_ONLY) {
            mDialogFragment.show(getSupportFragmentManager(), PLAYER_TAG);
        } else {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.left_dynamic_fragments_frame, mDialogFragment, PLAYER_TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     *
     * Show Playing Now section in the Action Bar
     */
    @Override
    public void showPlayNow(String playerStatus, String artistName, String albumName, String trackName) {
        mWasPlayNowVisible = true;
        playerControllerUiIdNotVisible();
        ActionBar actionBar = getSupportActionBar();
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.REGISTER_FOR_PLAY_NOW_EVENTS)
                        .build());
        createActionbarCustomView(actionBar, playerStatus, artistName, albumName, trackName);
    }

    private void createActionbarCustomView(ActionBar actionBar, String playerStatus, String artistName, String albumName, String trackName) {
        actionBar.setCustomView(R.layout.play_naw_action_bar);

        if (!mTwoPane && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_HOME_AS_UP);
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        playerStatusBt = (Button) actionBar.getCustomView().findViewById(R.id.playingNowId);
        playerStatusBt.setEnabled(true);
        playerStatusBt.setText(playerStatus);
        playerStatusBt.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                showPlayerController(-1, true);
            }
        });

        artistTv = (TextView) actionBar.getCustomView().findViewById(R.id.playingNowArtistsId);
        artistTv.setText(artistName);

        albumTv = (TextView) actionBar.getCustomView().findViewById(R.id.playingNowAlbumId);
        albumTv.setText(albumName);

        currTrackNameTv = (TextView) actionBar.getCustomView().findViewById(R.id.playingNowTrackNameId);
        currTrackNameTv.setText(trackName);
    }


    /**
     *
     * Remove Playing Now section from the Action Bar
     */
    @Override
    public void removePlayNow(String source) {
        mWasPlayNowVisible = false;
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.UNREGISTER_FOR_PLAY_NOW_EVENTS)
                        .build());

        if (!mTwoPane && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        }
        getSupportActionBar().setTitle(mActivityTitle);
        getSupportActionBar().setSubtitle(mActivitySubtitle);
    }

    /**
     * Player UI is not visible
     */
    @Override
    public void playerControllerUiIdNotVisible() {
        mWasPlayerControllerUiVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityTitle = getResources().getString(R.string.title_activity_artists);
        if (!mTwoPane && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = new Intent(getApplicationContext(), MusicPlayerService.class);
        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.UNREGISTER_FOR_PLAY_NOW_EVENTS)
                        .build());
        if (isMusicPlayerServiceBounded) {
            mMusicPlayerService.processBeforeDisconnectingFromService(false);
            unbindService(mServiceConnection);
            isMusicPlayerServiceBounded = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventBus.unregister(this);
    }

    private void processAfterConnectedToService() {
        mMusicPlayerService.processAfterConnectedToService(false);
        if (mWasPlayNowVisible) {
            eventBus.post(
                    new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.GET_PLAY_NOW_DATA)
                            .build());
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMusicPlayerService = ((MusicPlayerService.LocalBinder) service).getService();
            isMusicPlayerServiceBounded = true;
            processAfterConnectedToService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicPlayerService = null;
            isMusicPlayerServiceBounded = false;
        }
    };

    /**
     * Get messages through Event Bus from Green Robot
     */
    public void onEventMainThread(SpotifyStreamerActivityEvents event) {
        SpotifyStreamerActivityEvents.SpotifyStreamerEvents requestEvent = event.event;
        switch (requestEvent) {

            case CURR_TRACK_INFO:
                if (artistTv != null) {
                    artistTv.setText(event.currArtistName);
                }
                if (albumTv != null) {
                    albumTv.setText(event.trackDetails.albumName);
                }
                if (currTrackNameTv != null) {
                    currTrackNameTv.setText(event.trackDetails.trackName);
                }
                playerStatusBt.setText(event.currPlayerStatus);
                break;

            case SET_CURR_PLAY_NOW_DATA:
                showPlayNow(event.currPlayerStatus, event.currArtistName, event.trackDetails.albumName, event.trackDetails.trackName);
                break;

            case PLAYER_STAUS:
                playerStatusBt.setText(event.currPlayerStatus);
                break;

            default:
                throw new RuntimeException("LOC_CAT_TAG - onEvent - no code to handle requestEvent: " + requestEvent);
        }
    }
}
