package au.com.kbrsolutions.spotifystreamer.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
        void showProgress();
        void hideProgress();
    }

    private View playPause;
    private TextView playerTrackDuration;
    private SeekBar playerSeekBar;
    private TextView albumName;
    private ImageView albumImage;
    private TextView artistName;
    private TextView trackName;

    private String mArtistName;
    private ArrayList<TrackDetails> mTracksDetails;
    private int mSelectedTrackIdx;
//    private PlayerResultReceiver resultReceiver;
    private int mWidthPx = -1;
    private boolean isPlaying;
    private boolean mPlayClickedAtLeastOnceForCurrArtist;
    private boolean isMusicPlayerServiceBounded;
    private PlayerControllerUiCallbacks mCallbacks;
    private MusicPlayerService mMusicPlayerService;
    private EventBus eventBus;
    private static DecimalFormat dfTwoDecimalPlaces = new DecimalFormat("0.00");     // will format using default locale - use to format what is shown
                                                                                     // on the screen

    public final static String ARTIST_NAME = "artist_name";
    public final static String TRACKS_DETAILS = "tracks_details";
    public final static String SELECTED_TRACK = "selected_track";
    public final static String IS_PLAYING = "is_playing";

    public static final String PLAYER_RESULT_RECEIVER = "receiver";

    private final static String LOG_TAG = PlayerControllerUi.class.getSimpleName();

    /**
     * Create a new instance of PlayerControllerUi, providing "mTracksDetails" and "mSelectedTrackIdx"
     * as arguments.
     */
    public static PlayerControllerUi newInstance(String artistName, ArrayList<TrackDetails> tracksDetails, int selectedTrack) {
        PlayerControllerUi f = new PlayerControllerUi();

        Bundle args = new Bundle();
        args.putString(ARTIST_NAME, artistName);
        args.putParcelableArrayList(TRACKS_DETAILS, tracksDetails);
        args.putInt(SELECTED_TRACK, selectedTrack);
        f.setArguments(args);

        return f;
    }

    private Drawable playDrawable;
    private Drawable pauseDrawable;
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
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (playDrawable == null) {
            playDrawable = getResources().getDrawable(R.drawable.ic_action_play);
            pauseDrawable = getResources().getDrawable(R.drawable.ic_action_pause);
        }
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
        }
        setRetainInstance(true);
//        Log.v(LOG_TAG, "onCreate - mTracksDetails/mSelectedTrackIdx: " + mTracksDetails + "/" + mSelectedTrackIdx);
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
        artistName.setText(mArtistName);

        albumName = (TextView) playerView.findViewById(R.id.playerAlbumName);
        albumName.setText(mTracksDetails.get(mSelectedTrackIdx).albumName);

        albumImage = (ImageView) playerView.findViewById(R.id.playerAlbumImageView);

        if (mWidthPx == -1) {
            mWidthPx = (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                    (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
        }

        if (albumImage != null) {
            Picasso.with(getActivity())
                    .load(mTracksDetails.get(mSelectedTrackIdx).albumArtLargeImageUrl)
                    .resize(mWidthPx, mWidthPx).centerCrop()
                    .into(albumImage);
        }

        trackName = (TextView) playerView.findViewById(R.id.playerTrackName);
        trackName.setText(mTracksDetails.get(mSelectedTrackIdx).trackName);

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
//        playerTrackDuration.setText(mTracksDetails.get(mSelectedTrackIdx).trackName);

        final View playPrev = playerView.findViewById(R.id.playerPrev);
        playPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevClicked();
            }
        });

        playPause = playerView.findViewById(R.id.playerStartStopPlaying);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopClicked();
            }
        });
        if (isPlaying) {
            playPause.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_action_pause));
        } else {
            playPause.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_action_play));

        }

        final View playNext = playerView.findViewById(R.id.playerNext);
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextClicked();
            }
        });
        if (mMusicPlayerService == null) {
            startMusicServiceIfNotAlreadyBound();
        } else {
            mMusicPlayerService.reconnectedToMusicPlayerService();
        }
        return playerView;
    }

    /**
     * Save data if configuration changed - device rotation, etc.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(LOG_TAG, "onSaveInstanceState");
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
     * the track, most likely they will want to hear some tracks.
     */
    private void startMusicServiceIfNotAlreadyBound() {
//        Log.i(LOG_TAG, "startMusicServiceIfNotAlreadyBound - start - mServiceConnection/isMusicPlayerServiceBounded: " + mServiceConnection + "/" + isMusicPlayerServiceBounded);
        if (!isMusicPlayerServiceBounded) {
//            Log.v(LOG_TAG, "newTrackClicked - sending intent to service");
            Intent intent = new Intent(getActivity(), MusicPlayerService.class);
            getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(intent);
//            Log.v(LOG_TAG, "startMusicServiceIfNotAlreadyBound - sent intent to service - activity hash: " + resultReceiver.getActivityHashCode());
        } else {
//            Log.v(LOG_TAG, "startMusicServiceIfNotAlreadyBound - service is ALREADY bound");
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(LOG_TAG, "onServiceConnected - start");
            mMusicPlayerService = ((MusicPlayerService.LocalBinder) service).getService();
//            mMusicPlayerService.setPlayerControllerUi(super);
            isMusicPlayerServiceBounded = true;
//            mMusicPlayerService.setTracksDetails(mTracksDetails);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LOG_TAG, "onServiceDisconnected - start");
            mMusicPlayerService = null;
            isMusicPlayerServiceBounded = false;
        }
    };

    private void playPrevClicked() {
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_PREV_TRACK)
                        .build());
    }

    private void startStopClicked() {
        if (isPlaying) {
            mMusicPlayerService.pause();
        } else {
            if (mPlayClickedAtLeastOnceForCurrArtist) {
                mMusicPlayerService.resume();
            } else {
                mCallbacks.showProgress();
                isProgressBarShowing = true;
                Log.i(LOG_TAG, "startStopClicked - showProgress called");
                eventBus.post(
                        new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_TRACK)
                                .setTracksDetails(mTracksDetails)
                                .setSelectedTrack(mSelectedTrackIdx)
                                .build());
            }
        }
        isPlaying = !isPlaying;
        mPlayClickedAtLeastOnceForCurrArtist = true;
    }

    private void playNextClicked() {
        eventBus.post(
                new MusicPlayerServiceEvents.Builder(MusicPlayerServiceEvents.MusicServiceEvents.PLAY_NEXT_TRACK)
                        .build());
    }

    public void onEventMainThread(PlayerControllerUiEvents event) {
        PlayerControllerUiEvents.PlayerUiEvents request = event.event;
        switch (request) {
            case START_PLAYING_TRACK:
//                Log.v(LOG_TAG, "onEventMainThread - got request START_PLAYING_TRACK - activity/playPause: " + getActivity() + "/" + playPause + "/" + event.timeInSecs);
                if (getActivity() == null) {
                    Log.v(LOG_TAG, "onEventMainThread - activity is NULL");
                } else {

                    mCallbacks.hideProgress();
                    isProgressBarShowing = false;
                }
                playPause.setBackground(pauseDrawable);
                playerTrackDuration.setText(dfTwoDecimalPlaces.format(event.durationTimeInSecs));
//                playerSeekBar.setMax(event.durationTimeInSecs);
                playerSeekBar.setMax(100);      // 100%
                playerSeekBar.setProgress(0);
//                playPause.setBackground(getResources().getDrawable(R.drawable.ic_action_pause));        // java.lang.IllegalStateException: Fragment PlayerControllerUi{14dac624} not attached to Activity
                break;

            case PLAYING_TRACK:
                Log.v(LOG_TAG, "onEventMainThread - got request PLAYING_TRACK - activity/playPause: " + getActivity() + "/" + playPause);
                playPause.setBackground(pauseDrawable);
//                playPause.setBackground(getResources().getDrawable(R.drawable.ic_action_pause));        // java.lang.IllegalStateException: Fragment PlayerControllerUi{14dac624} not attached to Activity
                break;

            case PAUSED_TRACK:
                Log.v(LOG_TAG, "onEventMainThread - got request PAUSED_TRACK");
                playPause.setBackground(playDrawable);
                break;

            case TRACK_PLAY_PROGRESS:
//                Log.v(LOG_TAG, "onEventMainThread - got request TRACK_PLAY_PROGRESS - playProgressPercentage: " + event.playProgressPercentage);
                playerSeekBar.setProgress(event.playProgressPercentage);
                break;

            case PREPARING_NEXT_TRACK:
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
                if (getActivity() != null) {
                    mCallbacks.showProgress();
                    isProgressBarShowing = true;
                    Log.i(LOG_TAG, "onEvent - showProgress called");
                }
                albumName.setText(trackDetails.albumName);
        }
    }

    private boolean isProgressBarShowing;

    @Override
    public void onStop() {
        super.onStop();
//        Log.i(LOG_TAG, "onStop - start");
        // Unbind from the service
        if (isMusicPlayerServiceBounded) {
            getActivity().unbindService(mServiceConnection);
            isMusicPlayerServiceBounded = false;
        }
        if (isProgressBarShowing) {
            mCallbacks.hideProgress();
            Log.i(LOG_TAG, "onStop - hideProgress called");
            isProgressBarShowing = false;
        }
//        Log.i(LOG_TAG, "onStop - end");
    }

}
