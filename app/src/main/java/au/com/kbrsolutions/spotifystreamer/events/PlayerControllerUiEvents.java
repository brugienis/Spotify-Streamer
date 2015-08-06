package au.com.kbrsolutions.spotifystreamer.events;

import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 24/07/2015.
 */
public class PlayerControllerUiEvents {

    public enum PlayerUiEvents {
        START_PLAYING_TRACK,
        PLAYING_TRACK,
        PAUSED_TRACK,
        TRACK_PLAY_PROGRESS,
        PREPARING_TRACK,
        PROCESS_PLAYER_STATE
    }

    public final PlayerUiEvents event;
    public final int durationTimeInSecs;
    public final int playProgressPercentage;
    public final ArrayList<TrackDetails> tracksDetails;
    public final int selectedTrack;
    public final boolean isFirstTrackSelected;
    public final boolean isLastTrackSelected;
    public final boolean isTrackPlaying;
    public final boolean isTrackPausing;

    private PlayerControllerUiEvents(Builder builder) {
        this.event = builder.event;
        this.durationTimeInSecs = builder.durationTimeInSecs;
        this.playProgressPercentage = builder.playProgressPercentage;
        this.tracksDetails = builder.tracksDetails;
        this.selectedTrack = builder.selectedTrack;
        this.isFirstTrackSelected = builder.isFirstTrackSelected;
        this.isLastTrackSelected = builder.isLastTrackSelected;
        this.isTrackPlaying = builder.isTrackPlaying;
        this.isTrackPausing = builder.isTrackPausing;
    }

    public static class Builder {

        public Builder(PlayerControllerUiEvents.PlayerUiEvents event) {
            this.event = event;
        }

        private PlayerUiEvents event;
        private int durationTimeInSecs;
        private int playProgressPercentage;
        private ArrayList<TrackDetails> tracksDetails;
        private int selectedTrack;
        private boolean isFirstTrackSelected;
        private boolean isLastTrackSelected;
        private boolean isTrackPlaying;
        private boolean isTrackPausing;

        public Builder setDurationTimeInSecs(int durationTimeInSecs) {
            this.durationTimeInSecs = durationTimeInSecs;
            return this;
        }

        public Builder setPlayProgressPercentage(int playProgressPercentage) {
            this.playProgressPercentage = playProgressPercentage;
            return this;
        }

        public Builder setTracksDetails(ArrayList<TrackDetails> tracksDetails) {
            this.tracksDetails = tracksDetails;
            return this;
        }

        public Builder setSselectedTrack(int selectedTrack) {
            this.selectedTrack = selectedTrack;
            return this;
        }

        public Builder setIsFirstTrackSelected(boolean playingFirstTrack) {
            this.isFirstTrackSelected = playingFirstTrack;
            return this;
        }

        public Builder setIsLastTrackSelected(boolean playingLastTrack) {
            this.isLastTrackSelected = playingLastTrack;
            return this;
        }

        public Builder setIsTrackPlaying(boolean isTrackPlaying) {
            this.isTrackPlaying = isTrackPlaying;
            return this;
        }

        public Builder setIsTrackPausing(boolean isTrackPausing) {
            this.isTrackPausing = isTrackPausing;
            return this;
        }

        public PlayerControllerUiEvents build() {
            return new PlayerControllerUiEvents(this);
        }

    }
}
