package au.com.kbrsolutions.spotifystreamer.events;

/**
 * Created by business on 24/07/2015.
 */
public class PlayerControllerUiEvents {

    public enum PlayerUiEvents {START_PLAYING_TRACK, PLAYING_TRACK, PAUSED_TRACK, TRACK_PLAY_PROGRESS}

    public final PlayerUiEvents event;
    public final int durationTimeInSecs;
    public final int playProgressPercentage;

    private PlayerControllerUiEvents(PlayerUiEvents event, int durationTimeInSecs, int playProgressPercentage) {
        this.event = event;
        this.durationTimeInSecs = durationTimeInSecs;
        this.playProgressPercentage = playProgressPercentage;
    }

    public static class Builder {

        public Builder(PlayerControllerUiEvents.PlayerUiEvents event) {
            this.event = event;
        }

        private PlayerUiEvents event;
        private int durationTimeInSecs;
        private int playProgressPercentage;

        public Builder setDurationTimeInSecs(int durationTimeInSecs) {
            this.durationTimeInSecs = durationTimeInSecs;
            return this;
        }

        public Builder setPlayProgressPercentage(int playProgressPercentage) {
            this.playProgressPercentage = playProgressPercentage;
            return this;
        }

        public PlayerControllerUiEvents build() {
            return new PlayerControllerUiEvents(event, durationTimeInSecs, playProgressPercentage);
        }

    }
}
