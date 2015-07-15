package au.com.kbrsolutions.spotifystreamer.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import au.com.kbrsolutions.spotifystreamer.R;

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

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        View playerView = inflater.inflate(R.layout.player_ui, container, false);

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
