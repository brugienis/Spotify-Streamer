package au.com.kbrsolutions.spotifystreamer.events;

/**
 * Created by business on 13/08/2015.
 */
public class SpotifyStreamerActivityEvents {

    public enum SpotifyStreamerEvents {
        CURR_TRACK_NAME,
    }

    public final SpotifyStreamerEvents event;
    public final String currTrackName;

    private SpotifyStreamerActivityEvents(Builder builder) {
        this.event = builder.event;
        this.currTrackName = builder.currTrackName;
    }

    public static class Builder {

        public Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents event) {
            this.event = event;
        }

        private SpotifyStreamerEvents event;
        private String currTrackName;

        public Builder setCurrTrackName(String currTrackName) {
            this.currTrackName = currTrackName;
            return this;
        }

        public SpotifyStreamerActivityEvents build() {
            return new SpotifyStreamerActivityEvents(this);
        }

    }
}