package au.com.kbrsolutions.spotifystreamer.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;
import au.com.kbrsolutions.spotifystreamer.events.MusicPlayerServiceEvents;
import au.com.kbrsolutions.spotifystreamer.events.PlayerControllerUiEvents;
import au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService;
import de.greenrobot.event.EventBus;

/**
 * Created by business on 14/07/2015.
 */
public class PlayerControllerUi extends DialogFragment {

    /**
     * Declares callback methods that have to be implemented by parent Activity
     */
    public interface PlayerControllerUiCallbacks {
        void playerControllerUiIdNotVisible();
        void showPlayNow(String playerStatus, String artistName, String albumName, String trackName);
        void removePlayNow(String source);
        boolean wasPlayerControllerUiVisibleOnRestart();
        void setWasPlayerControllerUiVisible(boolean value);
    }

    private View playPause;
    private ProgressBar playPauseProgressBar;
    private TextView playerTrackDuration;
    private SeekBar playerSeekBar;
    private View playPrev;
    private View playNext;
    private TextView albumName;
    private ImageView albumImage;
    private TextView artistName;
    private TextView trackName;
    private Drawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
    private Drawable playPrevDrawable;
    private Drawable playCurrentDrawable;
    private Drawable pauseCurrentDrawable;
    private Drawable playNextDrawable;
    private Drawable playDrawable;
    private Drawable pauseDrawable;

    private String mArtistName;
    private ArrayList<TrackDetails> mTracksDetails;
    private int mSelectedTrackIdx;
    private int mWidthPx = -1;
    private boolean isPlaying;
    private boolean isPausing;
    private boolean mReconnectToPlayerService;
    private boolean isMusicPlayerServiceBounded;
    private boolean isThisSessionOnCreateCalled;
    private PlayerControllerUiCallbacks mCallbacks;
    private MusicPlayerService mMusicPlayerService;
    private EventBus eventBus;
    private static DecimalFormat dfTwoDecimalPlaces = new DecimalFormat("0.00");     // will format using default locale - use to format what is shown on the screen
    public final static String ARTIST_NAME = "artist_name";
    public final static String TRACKS_DETAILS = "tracks_details";
    public final static String SELECTED_TRACK = "selected_track";
    public final static String RECONNECT_TO_PLAYER_SERVICE = "reconnect_to_player_service";
    public final static String IS_PLAYING = "is_playing";

    private final static String LOG_TAG = PlayerControllerUi.class.getSimpleName();

    /**
     * Create a new instance of PlayerControllerUi, providing "mTracksDetails" and "mSelectedTrackIdx"
     * as arguments.
     */
    public static PlayerControllerUi newInstance(String artistName,
                                                 ArrayList<TrackDetails> tracksDetails,
                                                 int selectedTrack,
                                                 boolean reconnectToPlayerService) {
        PlayerControllerUi f = new PlayerControllerUi();

        Bundle args = new Bundle();
        args.putString(ARTIST_NAME, artistName);
        args.putParcelableArrayList(TRACKS_DETAILS, tracksDetails);
        args.putInt(SELECTED_TRACK, selectedTrack);
        args.putBoolean(RECONNECT_TO_PLAYER_SERVICE, reconnectToPlayerService);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        try {
            mCallbacks = (PlayerControllerUiCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException(
                    activity.getResources()
                            .getString(R.string.callbacks_not_implemented, activity.toString()));
        }
        if (eventBus == null) {
            eventBus = EventBus.getDefault();
            eventBus.register(this);
        }
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
        playDrawable = getResources().getDrawable(R.drawable.ic_action_play);
        pauseDrawable = getResources().getDrawable(R.drawable.ic_action_pause);
        playPrevDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_previous);
        playCurrentDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_play);
        pauseCurrentDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_pause);
        playNextDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_next);
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isThisSessionOnCreateCalled = true;

        if (getArguments() != null) {
            if (getArguments().containsKey(TRACKS_DETAILS)) {
                mTracksDetails = getArguments().getParcelableArrayList(TRACKS_DETAILS);
            }
            if (getArguments().containsKey(ARTIST_NAME)) {
                mArtistName = getArguments().getString(ARTIST_NAME);
            }
            if (getArguments().containsKey(SELECTED_TRACK)) {
                mSelectedTrackIdx = getArguments().getInt(SELECTED_TRACK);
            }
            if (getArguments().containsKey(RECONNECT_TO_PLAYER_SERVICE)) {
                mReconnectToPlayerService = getArguments().getBoolean(RECONNECT_TO_PLAYER_SERVICE);
            }
        }
        setRetainInstance(true);
    }

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment

        if (savedInstanceState != null) {
            mReconnectToPlayerService = true;   // configuration changed?
            if (savedInstanceState.containsKey(ARTIST_NAME)) {
                mArtistName = savedInstanceState.getString(ARTIST_NAME);
            }
            if (savedInstanceState.containsKey(TRACKS_DETAILS)) {
                mTracksDetails = savedInstanceState.getParcelableArrayList(TRACKS_DETAILS);
            }
            if (savedInstanceState.containsKey(SELECTED_TRACK)) {
                mSelectedTrackIdx = savedInstanceState.getInt(SELECTED_TRACK);
            }
            if (savedInstanceState.containsKey(IS_PLAYING)) {
                isPlaying = savedInstanceState.getBoolean(IS_PLAYING);
            }
        }

        View playerView = inflater.inflate(R.layout.player_ui, container, false);

        artistName = (TextView) playerView.findViewById(R.id.playerArtistName);

        albumName = (TextView) playerView.findViewById(R.id.playerAlbumName);

        albumImage = (ImageView) playerView.findViewById(R.id.playerAlbumImageView);

        if (mWidthPx == -1) {
            mWidthPx = (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                    (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
        }

        trackName = (TextView) playerView.findViewById(R.id.playerTrackName);

        playerSeekBar =  (SeekBar) playerView.findViewById(R.id.playerSeekBar);
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    Log.v(LOG_TAG, "onProgressChanged - progress/fromUser: " + progress + "/" + fromUser);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        playerTrackDuration = (TextView) playerView.findViewById(R.id.playerTrackDuration);

        playPrev = playerView.findViewById(R.id.playerPrev);

        playPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevClicked();
            }
        });

        playPauseProgressBar = (ProgressBar) playerView.findViewById(R.id.playerProgressBar);

        playPause = playerView.findViewById(R.id.playerStartStopPlaying);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseClicked();
            }
        });

        playNext = playerView.findViewById(R.id.playerNext);
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextClicked();
            }
        });

        if (!mReconnectToPlayerService) {
            showCurrentTrackDetails(
                    mTracksDetails.get(mSelectedTrackIdx),
                    false,
                    false,
                    mSelectedTrackIdx == 0,
                    mSelectedTrackIdx == mTracksDetails.size() - 1,
                    -1,
                    -1);
        }

        return playerView;
    }

    /**
     *
     * Populate UI fields.
     */
    private void showCurrentTrackDetails(
            TrackDetails trackDetails,
            boolean isTrackPlaying,
            boolean isTrackPausing,
            boolean isFirstTrackSelected,
            boolean isLastTrackSelected,
            int progressPercentage,
            int durationTimeInSecs) {
        artistName.setText(mArtistName);

        albumName.setText(trackDetails.albumName);

        if (albumImage != null) {
            Picasso.with(getActivity())
                    .load(trackDetails.albumArtLargeImageUrl)
                    .resize(mWidthPx, mWidthPx).centerCrop()
                    .into(albumImage);
        }

        trackName.setText(trackDetails.trackName);

        if (isFirstTrackSelected) {
            playPrev.setBackground(transparentDrawable);
        } else {
            playPrev.setBackground(playPrevDrawable);
        }

        if (progressPercentage > -1) {
            playerSeekBar.setProgress(progressPercentage);
        }
        playerSeekBar.setProgress(progressPercentage);
        if (durationTimeInSecs > 0) {
            playerTrackDuration.setText(dfTwoDecimalPlaces.format(durationTimeInSecs));
        }

        playPause.setVisibility(View.VISIBLE);
        playPauseProgressBar.setVisibility(View.GONE);
        if (isTrackPlaying) {
            playPause.setBackground(pauseCurrentDrawable);
        } else if (isTrackPausing) {
            playPause.setBackground(playCurrentDrawable);
        } else {
            playPause.setVisibility(View.GONE);
            playPauseProgressBar.setVisibility(View.VISIBLE);
        }

        if (isLastTrackSelected) {
            playNext.setBackground(transparentDrawable);
        } else {
            playNext.setBackground(playNextDrawable);
        }

    }

    /**
     * Save data if configuration changed - device rotation, etc.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARTIST_NAME, mArtistName);
        if (mTracksDetails != null) {
            outState.putParcelableArrayList(TRACKS_DETAILS, (ArrayList) mTracksDetails);
        }
        outState.putInt(SELECTED_TRACK, mSelectedTrackIdx);
        outState.putBoolean(IS_PLAYING, Boolean.valueOf(isPlaying));
        super.onSaveInstanceState(outState);
    }



    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     *
     */
    private void connectToMusicPlayerService() {
        if (mMusicPlayerService == null) {
            Intent intent = new Intent(getActivity(), MusicPlayerService.class);
            getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(intent);
        } else {
            initialProcessingAfterConnectingToService();
        }
    }

    private void initialProcessingAfterConnectingToService() {
        mMusicPlayerService.processAfterConnectedToService(true);
        if (mReconnectToPlayerService) {
            eventBus.post(
                    new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.GET_PLAYER_STATE_DETAILS)
                            .build());
            mReconnectToPlayerService = false;
        } else {
            setTracksDetailsAndPlaySelectedTrack();
        }
    }

    private void setTracksDetailsAndPlaySelectedTrack() {
        mMusicPlayerService.setTracksDetails(mArtistName, mTracksDetails, mSelectedTrackIdx);
        playPause.setEnabled(false);
        playPause.setVisibility(View.GONE);
        playPauseProgressBar.setVisibility(View.VISIBLE);
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_TRACK)
                        .build());
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMusicPlayerService = ((MusicPlayerService.LocalBinder) service).getService();
            isMusicPlayerServiceBounded = true;
            initialProcessingAfterConnectingToService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicPlayerService = null;
            isMusicPlayerServiceBounded = false;
        }
    };

    private void playPrevClicked() {
        if (mSelectedTrackIdx == 1) {
            mSelectedTrackIdx--;
            playPrev.setEnabled(false);
            playPrev.setBackground(transparentDrawable);
        }
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_PREV_TRACK)
                        .build());
    }

    /**
     * Normally isPlaying is the oposite isPausing. But, when Player UI gets its state from the MusicPlayerService, they both could be false.
     * It can happen is the OnPrepare is active. If that is happening the playPause should be invisible and the progress bar visible.
     */
    private void playPauseClicked() {
        if (isPlaying) {
            mMusicPlayerService.pause();
        } else if (isPausing) {
//            if (mPlayClickedAtLeastOnceForCurrArtist) {
            mMusicPlayerService.resume();
        } else {
            return;
        }
        isPlaying = !isPlaying;
        isPausing = !isPausing;
    }

    private void playNextClicked() {
        if (mSelectedTrackIdx == mTracksDetails.size() - 2) {
            mSelectedTrackIdx++;
            playNext.setEnabled(false);
            playNext.setBackground(transparentDrawable);
        }
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_NEXT_TRACK)
                        .build());
    }

    /**
     * Get messages through Event Bus from Green Robot
     */
    public void onEventMainThread(PlayerControllerUiEvents event) {
        PlayerControllerUiEvents.PlayerUiEvents request = event.event;
        switch (request) {
            case START_PLAYING_TRACK:
                isPlaying = true;
                playPause.setEnabled(true);
                playPause.setBackground(pauseDrawable);
                playPause.setVisibility(View.VISIBLE);
                playPauseProgressBar.setVisibility(View.GONE);
                playerTrackDuration.setText(dfTwoDecimalPlaces.format(event.durationTimeInSecs));
                playerSeekBar.setMax(100);      // 100%
                playerSeekBar.setProgress(0);
                break;

            case PLAYING_TRACK:
                playPause.setBackground(pauseDrawable);
                break;

            case PAUSED_TRACK:
                playPause.setBackground(playDrawable);
                break;

            case TRACK_PLAY_PROGRESS:
                playerSeekBar.setProgress(event.playProgressPercentage);
                break;

            case PREPARING_TRACK:
                mSelectedTrackIdx = event.selectedTrack;
                TrackDetails trackDetails = mTracksDetails.get(mSelectedTrackIdx);
                trackName.setText(trackDetails.trackName);
                playerSeekBar.setProgress(0);
                String imageUrl = trackDetails.albumArtLargeImageUrl;
                if (imageUrl != null) {
                    Picasso.with(getActivity())
                            .load(mTracksDetails.get(mSelectedTrackIdx).albumArtLargeImageUrl)
                            .resize(mWidthPx, mWidthPx).centerCrop()
                            .into(albumImage);
                }
                playPause.setBackground(transparentDrawable);
                playPause.setEnabled(false);
                playPause.setVisibility(View.GONE);
                playPauseProgressBar.setVisibility(View.VISIBLE);
                if (event.isFirstTrackSelected) {
                    playPrev.setEnabled(false);
                    playPrev.setBackground(transparentDrawable);
                } else {
                    playPrev.setEnabled(true);
                    playPrev.setBackground(playPrevDrawable);
                }

                if (event.isLastTrackSelected) {
                    playNext.setEnabled(false);
                    playNext.setBackground(transparentDrawable);
                } else {
                    playNext.setEnabled(true);
                    playNext.setBackground(playNextDrawable);
                }
                albumName.setText(trackDetails.albumName);
                break;

            case PROCESS_PLAYER_STATE:
                if (event.tracksDetails == null) {
                    mSelectedTrackIdx = 0;
                    showCurrentTrackDetails(
                            mTracksDetails.get(mSelectedTrackIdx),
                            false,
                            false,
                            mSelectedTrackIdx == 0,
                            mSelectedTrackIdx == mTracksDetails.size() - 1,
                            -1,
                            -1);
                    setTracksDetailsAndPlaySelectedTrack();
                } else {
                    mTracksDetails = event.tracksDetails;
                    mSelectedTrackIdx = event.selectedTrack;
                    isPlaying = event.isTrackPlaying;
                    isPausing = event.isTrackPausing;
                    showCurrentTrackDetails(
                            event.tracksDetails.get(mSelectedTrackIdx),
                            event.isTrackPlaying,
                            event.isTrackPausing,
                            event.isFirstTrackSelected,
                            event.isLastTrackSelected,
                            event.playProgressPercentage,
                            event.durationTimeInSecs
                    );
                }
                break;
        }
    }

    /**
     * Sets a flag that indicates the Player UI is reconnecting to the Music Player service.
     */
    public void setReconnectToPlayerService() {
        mReconnectToPlayerService = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallbacks.removePlayNow("onResume " + hashCode());
        if (mCallbacks.wasPlayerControllerUiVisibleOnRestart() || !isThisSessionOnCreateCalled) {
            mReconnectToPlayerService = true;
        }
        mCallbacks.setWasPlayerControllerUiVisible(true);
        connectToMusicPlayerService();
    }

    /**
     * if this class was created because of configuration change, don't call processBeforeDisconnectingFromService()
     */
    @Override
    public void onStop() {
        super.onStop();
        isThisSessionOnCreateCalled = false;
        if (isMusicPlayerServiceBounded) {
            getActivity().unbindService(mServiceConnection);
            isMusicPlayerServiceBounded = false;
        }
        mMusicPlayerService.processBeforeDisconnectingFromService(true);
        TrackDetails trackDetails = mTracksDetails.get(mSelectedTrackIdx);
        String playerStatus = isPlaying ?
                getString(R.string.player_playing) :
                isPausing ?
                        getResources().getString(R.string.player_paused) : getString(R.string.player_preparing);
        mCallbacks.showPlayNow(playerStatus, mArtistName, trackDetails.albumName, trackDetails.trackName);
        mCallbacks.setWasPlayerControllerUiVisible(false);
    }

    /**
     * on device rotation, the Player UI would automatically appear and then would disappear. Followed advice in the:
     *     http://stackoverflow.com/questions/12433397/android-dialogfragment-disappears-after-orientation-change
     */
    @Override
    public void onDestroyView() {
        /* the 'if' block below solves the problem */
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
        mCallbacks.playerControllerUiIdNotVisible();
        eventBus.unregister(this);
    }

}