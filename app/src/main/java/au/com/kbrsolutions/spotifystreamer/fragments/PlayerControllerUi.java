package au.com.kbrsolutions.spotifystreamer.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 14/07/2015.
 */
public class PlayerControllerUi extends DialogFragment {

    /**
     * Declares callback methods that have to be implemented by parent Activity
     */
    public interface PlayerControllerUiCallbacks {
        void playPreviousTrack();
        void startStopPlaying();
        void playNextTrack();
    }

    private PlayerControllerUiCallbacks mCallbacks;
    private ArrayList<TrackDetails> tracksDetails;
    private int selectedTrack;
    private int mWidthPx = -1;
    public final static String TRACKS_DETAILS = "tracks_details";
    public final static String SELECTED_TRACK = "selected_track";

    private final static String LOG_TAG = PlayerControllerUi.class.getSimpleName();

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    public static PlayerControllerUi newInstance(ArrayList<TrackDetails> tracksDetails, int selectedTrack) {
        PlayerControllerUi f = new PlayerControllerUi();

        Bundle args = new Bundle();
        args.putParcelableArrayList(TRACKS_DETAILS, tracksDetails);
        args.putInt(SELECTED_TRACK, selectedTrack);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        try {
            mCallbacks = (PlayerControllerUiCallbacks) activity;
        } catch (Exception e) {
            throw new RuntimeException(
                    getActivity().getResources()
                            .getString(R.string.callbacks_not_implemented, activity.toString()));
        }
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "onCreate - start");

        if (getArguments() != null) {
            if (getArguments().containsKey(TRACKS_DETAILS)) {
                tracksDetails = getArguments().getParcelableArrayList(TRACKS_DETAILS);
            }
            if (getArguments().containsKey(SELECTED_TRACK)) {
                selectedTrack = getArguments().getInt(SELECTED_TRACK);
            }
        }
//        Log.v(LOG_TAG, "onCreate - tracksDetails/selectedTrack: " + tracksDetails + "/" + selectedTrack);
    }

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        Log.v(LOG_TAG, "onCreateView - start");
        View playerView = inflater.inflate(R.layout.player_ui, container, false);

        final TextView artistName = (TextView) playerView.findViewById(R.id.playerArtistName);
        artistName.setText(tracksDetails.get(selectedTrack).albumName);

        final TextView albumName = (TextView) playerView.findViewById(R.id.playerAlbumName);
        albumName.setText(tracksDetails.get(selectedTrack).albumName);

        final ImageView albumImage = (ImageView) playerView.findViewById(R.id.playerAlbumImageView);

        if (mWidthPx == -1) {
            mWidthPx = (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_size) -
                    (int) getActivity().getResources().getDimension(R.dimen.artist_thumbnail_image_padding);
            if (albumImage != null) {
                Picasso.with(getActivity())
                        .load(tracksDetails.get(selectedTrack).albumArtLargeImageUrl)
                        .resize(mWidthPx, mWidthPx).centerCrop()
                        .into(albumImage);
            }
        }

        final TextView trackName = (TextView) playerView.findViewById(R.id.playerTrackName);
        trackName.setText(tracksDetails.get(selectedTrack).trackName);

        final View playPrev = playerView.findViewById(R.id.playerPrev);
        playPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevClicked();
            }
        });

        final View startStop = playerView.findViewById(R.id.playerStartStopPlaying);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopClicked();
            }
        });

        final View playNext = playerView.findViewById(R.id.playerNext);
        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextClicked();
            }
        });

        return playerView;
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

    private void playPrevClicked() {
        mCallbacks.playPreviousTrack();
    }

    private void startStopClicked() {
        mCallbacks.startStopPlaying();
    }

    private void playNextClicked() {
        mCallbacks.playNextTrack();
    }
}
