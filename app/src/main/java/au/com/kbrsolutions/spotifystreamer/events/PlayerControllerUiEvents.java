package au.com.kbrsolutions.spotifystreamer.events;

/**
 * Created by business on 24/07/2015.
 */
public class PlayerControllerUiEvents {

    public enum PlayerUiEvents {
        START_PLAYING_TRACK,
        PLAYING_TRACK,
        PAUSED_TRACK,
        TRACK_PLAY_PROGRESS,
        PREPARING_PREV_NEXT_TRACK
    }

    public final PlayerUiEvents event;
    public final int durationTimeInSecs;
    public final int playProgressPercentage;
    public final int selectedTrack;
    public final boolean playingFirstTrack;
    public final boolean playingLastTrack;

    private PlayerControllerUiEvents(Builder builder) {
        this.event = builder.event;
        this.durationTimeInSecs = builder.durationTimeInSecs;
        this.playProgressPercentage = builder.playProgressPercentage;
        this.selectedTrack = builder.selectedTrack;
        this.playingFirstTrack = builder.playingFirstTrack;
        this.playingLastTrack = builder.playingLastTrack;
    }

    public static class Builder {

        public Builder(PlayerControllerUiEvents.PlayerUiEvents event) {
            this.event = event;
        }

        private PlayerUiEvents event;
        private int durationTimeInSecs;
        private int playProgressPercentage;
        private int selectedTrack;
        private boolean playingFirstTrack;
        private boolean playingLastTrack;

        public Builder setDurationTimeInSecs(int durationTimeInSecs) {
            this.durationTimeInSecs = durationTimeInSecs;
            return this;
        }

        public Builder setPlayProgressPercentage(int playProgressPercentage) {
            this.playProgressPercentage = playProgressPercentage;
            return this;
        }

        public Builder setSselectedTrack(int selectedTrack) {
            this.selectedTrack = selectedTrack;
            return this;
        }

        public Builder setPlayingFirstTrack(boolean playingFirstTrack) {
            this.playingFirstTrack = playingFirstTrack;
            return this;
        }

        public Builder setPlayingLastTrack(boolean playingLastTrack) {
            this.playingLastTrack = playingLastTrack;
            return this;
        }

        public PlayerControllerUiEvents build() {
            return new PlayerControllerUiEvents(this);
        }

    }
}
