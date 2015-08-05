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
import android.util.Log;
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
//        void showProgress();
//        void hideProgress();
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
    private boolean mReconnectToPlayerService;
    private boolean mPlayClickedAtLeastOnceForCurrArtist;
    private boolean isMusicPlayerServiceBounded;
    private boolean isProgressBarShowing;
    private PlayerControllerUiCallbacks mCallbacks;
    private MusicPlayerService mMusicPlayerService;
    private EventBus eventBus;
    private static DecimalFormat dfTwoDecimalPlaces = new DecimalFormat("0.00");     // will format using default locale - use to format what is shown on the screen
    public final static String ARTIST_NAME = "artist_name";
    public final static String TRACKS_DETAILS = "tracks_details";
    public final static String SELECTED_TRACK = "selected_track";
    public final static String RECONNECT_TO_PLAYER_SERVICE = "reconnect_to_player_service";
    public final static String IS_PLAYING = "is_playing";

    public static final String PLAYER_RESULT_RECEIVER = "receiver";

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
//        Log.v(LOG_TAG, "onAttach - start");
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
        if (playDrawable == null) {
            playDrawable = getResources().getDrawable(R.drawable.ic_action_play);
            pauseDrawable = getResources().getDrawable(R.drawable.ic_action_pause);
            playPrevDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_previous);
            playCurrentDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_play);
            pauseCurrentDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_pause);
            playNextDrawable = getActivity().getResources().getDrawable(R.drawable.ic_action_next);
        }
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.v(LOG_TAG, "onCreate - start");

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

        Log.v(LOG_TAG, "onCreate - mSelectedTrackIdx: " + mSelectedTrackIdx);
    }

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
//        Log.v(LOG_TAG, "onCreateView - start - savedInstanceState: " + savedInstanceState);

        if (savedInstanceState != null) {
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
//        artistName.setText(mArtistName);

        albumName = (TextView) playerView.findViewById(R.id.playerAlbumName);
//        albumName.setText(mTracksDetails.get(mSelectedTrackIdx).albumName);

        albumImage = (ImageView) playerView.findViewById(R.id.playerAlbumImageView);

        if (mWidthPx == -1) {
            mWidthPx = (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                    (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
        }

//        if (albumImage != null) {
//            Picasso.with(getActivity())
//                    .load(mTracksDetails.get(mSelectedTrackIdx).albumArtLargeImageUrl)
//                    .resize(mWidthPx, mWidthPx).centerCrop()
//                    .into(albumImage);
//        }

        trackName = (TextView) playerView.findViewById(R.id.playerTrackName);
//        trackName.setText(mTracksDetails.get(mSelectedTrackIdx).trackName);

        playerSeekBar =  (SeekBar) playerView.findViewById(R.id.playerSeekBar);
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Log.v(LOG_TAG, "onProgressChanged - progress/fromUser: " + progress + "/" + fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.v(LOG_TAG, "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v(LOG_TAG, "onStopTrackingTouch");
            }
        });

        playerTrackDuration = (TextView) playerView.findViewById(R.id.playerTrackDuration);

        playPrev = playerView.findViewById(R.id.playerPrev);

//        if (mSelectedTrackIdx == 0) {
//            playPrev.setBackground(transparentDrawable);
//        } else {
//            playPrev.setBackground(playPrevDrawable);
//        }
        playPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevClicked();
            }
        });

        playPauseProgressBar = (ProgressBar) playerView.findViewById(R.id.playerProgressBar);
//        playPauseProgressBar.setVisibility(View.GONE);

        playPause = playerView.findViewById(R.id.playerStartStopPlaying);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseClicked();
            }
        });
//        if (isPlaying) {
//            Log.v(LOG_TAG, "onCreateView - isPlaying");
//            playPause.setBackground(pauseCurrentDrawable);
//        } else {
//            Log.v(LOG_TAG, "onCreateView - isPlaying NOT");
//            playPause.setBackground(playCurrentDrawable);
//        }

        playNext = playerView.findViewById(R.id.playerNext);
//        if (mSelectedTrackIdx == mTracksDetails.size() - 1) {
//            playNext.setBackground(transparentDrawable);
//        } else {
//            playNext.setBackground(playNextDrawable);
//        }
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextClicked();
            }
        });

//        if (mReconnectToPlayerService) {
//            Log.v(LOG_TAG, "onCreateView - before mMusicPlayerService: " + mMusicPlayerService);
//            eventBus.post(
//                    new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.GET_CURR_TRACKS_DETAILS)
//                            .build());
//            Log.v(LOG_TAG, "onCreateView - after  mMusicPlayerService: " + mMusicPlayerService);
//        }

        if (!mReconnectToPlayerService) {
            showCurrentTrackDetails(mTracksDetails.get(mSelectedTrackIdx), -1);
        }

        if (mMusicPlayerService == null) {
            startMusicServiceIfNotAlreadyBound();
        }


        return playerView;
    }

    private void showCurrentTrackDetails(TrackDetails trackDetails, int durationTimeInSecs) {
        artistName.setText(mArtistName);

        albumName.setText(trackDetails.albumName);

        if (albumImage != null) {
            Picasso.with(getActivity())
                    .load(trackDetails.albumArtLargeImageUrl)
                    .resize(mWidthPx, mWidthPx).centerCrop()
                    .into(albumImage);
        }

        trackName.setText(trackDetails.trackName);

        if (mSelectedTrackIdx == 0) {
            playPrev.setBackground(transparentDrawable);
        } else {
            playPrev.setBackground(playPrevDrawable);
        }

        playPauseProgressBar.setVisibility(View.GONE);
        if (durationTimeInSecs > 0) {
            playerTrackDuration.setText(dfTwoDecimalPlaces.format(durationTimeInSecs));
        }

        if (isPlaying) {
            playPause.setBackground(pauseCurrentDrawable);
        } else {
            playPause.setBackground(playCurrentDrawable);
        }

        if (mSelectedTrackIdx == mTracksDetails.size() - 1) {
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
//        Log.v(LOG_TAG, "onSaveInstanceState");
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
     * Starts MusicService as soon as possible - if it wasn't already started - when users clicks on
     * the track, most likely they will want to play them.
     */
    private void startMusicServiceIfNotAlreadyBound() {
        Log.i(LOG_TAG, "startMusicServiceIfNotAlreadyBound - start - mMusicPlayerService/isMusicPlayerServiceBounded: " + mMusicPlayerService + "/" + isMusicPlayerServiceBounded);
//        if (!isMusicPlayerServiceBounded) {
        if (mMusicPlayerService == null) {
            Intent intent = new Intent(getActivity(), MusicPlayerService.class);
            getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(intent);
            Log.v(LOG_TAG, "startMusicServiceIfNotAlreadyBound - sent intent to service - mSelectedTrackIdx: " + mSelectedTrackIdx);
        } else {
            passTracksdetailsToService();
            Log.v(LOG_TAG, "startMusicServiceIfNotAlreadyBound - service is ALREADY bound");
        }
    }

    private void passTracksdetailsToService() {
//        Log.i(LOG_TAG, "passTracksdetailsToService - mSelectedTrackIdx: " + mSelectedTrackIdx);
        if (mReconnectToPlayerService) {
            Log.v(LOG_TAG, "passTracksdetailsToService - mReconnectToPlayerService before mMusicPlayerService: " + mMusicPlayerService);
            eventBus.post(
                    new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.GET_CURR_TRACKS_DETAILS)
                            .build());
            Log.v(LOG_TAG, "passTracksdetailsToService - mReconnectToPlayerService after  mMusicPlayerService: " + mMusicPlayerService);
            return;
        }
//        if (mReconnectToPlayerService) {
//            return;
//        }
        mMusicPlayerService.processAfterConnectedToService();
        mMusicPlayerService.setTracksDetails(mTracksDetails, mSelectedTrackIdx);
        isPlaying = true;
        isProgressBarShowing = true;
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
            Log.i(LOG_TAG, "onServiceConnected - start");
            mMusicPlayerService = ((MusicPlayerService.LocalBinder) service).getService();
            isMusicPlayerServiceBounded = true;
            passTracksdetailsToService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LOG_TAG, "onServiceDisconnected - start");
            mMusicPlayerService = null;
            isMusicPlayerServiceBounded = false;
        }
    };

    private void playPrevClicked() {
        Log.i(LOG_TAG, "playPrevClicked - start");
        if (mSelectedTrackIdx == 1) {
            mSelectedTrackIdx--;
            playPrev.setEnabled(false);
            playPrev.setBackground(transparentDrawable);
        }
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_PREV_TRACK)
                        .build());
    }

    private void playPauseClicked() {
        if (isPlaying) {
            mMusicPlayerService.pause();
        } else {
            if (mPlayClickedAtLeastOnceForCurrArtist) {
                mMusicPlayerService.resume();
            } else {
                // TODO: 3/08/2015 - how can we go into that part of code - investigate
//                mCallbacks.showProgress();
//                isProgressBarShowing = true;
                playPrev.setEnabled(true);
                playNext.setEnabled(true);
//                Log.i(LOG_TAG, "playPauseClicked - mSelectedTrackIdx/size: " + mSelectedTrackIdx + "/" +  mTracksDetails.size());
                if (mSelectedTrackIdx == 0) {
                    playPrev.setEnabled(false);
                    playPrev.setBackground(transparentDrawable);
                } else if (mSelectedTrackIdx == mTracksDetails.size() - 1) {
                    playNext.setEnabled(false);
                    playNext.setBackground(transparentDrawable);
                }
                playPause.setBackground(transparentDrawable);
                eventBus.post(
                        new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_TRACK)
                                .build());
            }
        }
        isPlaying = !isPlaying;
        mPlayClickedAtLeastOnceForCurrArtist = true;
    }

    private void playNextClicked() {
        Log.i(LOG_TAG, "playNextClicked - start");
        if (mSelectedTrackIdx == mTracksDetails.size() - 2) {
            mSelectedTrackIdx++;
            playNext.setEnabled(false);
            playNext.setBackground(transparentDrawable);
        }
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_NEXT_TRACK)
                        .build());
    }

    public void onEventMainThread(PlayerControllerUiEvents event) {
        PlayerControllerUiEvents.PlayerUiEvents request = event.event;
        switch (request) {
            case START_PLAYING_TRACK:
                playPause.setEnabled(true);
                isProgressBarShowing = false;
                playPause.setBackground(pauseDrawable);
                playPause.setVisibility(View.VISIBLE);
                playPauseProgressBar.setVisibility(View.GONE);
                playerTrackDuration.setText(dfTwoDecimalPlaces.format(event.durationTimeInSecs));
                playerSeekBar.setMax(100);      // 100%
                playerSeekBar.setProgress(0);
                break;

            case PLAYING_TRACK:
//                Log.v(LOG_TAG, "onEventMainThread - got request PLAYING_TRACK - activity/playPause: " + getActivity() + "/" + playPause);
                playPause.setBackground(pauseDrawable);
                break;

            case PAUSED_TRACK:
//                Log.v(LOG_TAG, "onEventMainThread - got request PAUSED_TRACK");
                playPause.setBackground(playDrawable);
                break;

            case TRACK_PLAY_PROGRESS:
//                Log.v(LOG_TAG, "onEventMainThread - got request TRACK_PLAY_PROGRESS - playProgressPercentage: " + event.playProgressPercentage);
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
                if (event.playingFirstTrack) {
                    playPrev.setEnabled(false);
                    playPrev.setBackground(transparentDrawable);
//                    Log.i(LOG_TAG, "onEvent - playPrev disabled");
                } else {
                    playPrev.setEnabled(true);
                    playPrev.setBackground(playPrevDrawable);
                }

                if (event.playingLastTrack) {
                    playNext.setEnabled(false);
                    playNext.setBackground(transparentDrawable);
//                    Log.i(LOG_TAG, "onEvent - playNext disabled");
                } else {
                    playNext.setEnabled(true);
                    playNext.setBackground(playNextDrawable);
                }
                albumName.setText(trackDetails.albumName);
                break;

            case SHOW_CURR_TRACK_DETAILS:
                int selectedTrackIdx = event.selectedTrack;
//                TrackDetails trackDetails = event.tracksDetails.get(selectedTrackIdx);
                showCurrentTrackDetails(event.tracksDetails.get(selectedTrackIdx), event.durationTimeInSecs);
                break;
        }
    }

    /**
     * if this class was created because of configuration change, don't call processBeforeUnbindService()
     */
    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "onStop - start - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
        // Unbind from the service
        if (isMusicPlayerServiceBounded) {
            if (!mReconnectToPlayerService) {
                mMusicPlayerService.processBeforeUnbindService();
            }
            getActivity().unbindService(mServiceConnection);
            isMusicPlayerServiceBounded = false;
        }
        if (isProgressBarShowing) {
            Log.i(LOG_TAG, "onStop - hideProgress called");
        }
        Log.i(LOG_TAG, "onStop - end - isMusicPlayerServiceBounded: " + isMusicPlayerServiceBounded);
    }

}