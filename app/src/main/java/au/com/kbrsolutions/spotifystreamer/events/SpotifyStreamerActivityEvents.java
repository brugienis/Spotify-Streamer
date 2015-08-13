package au.com.kbrsolutions.spotifystreamer.events;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 13/08/2015.
 */
public class SpotifyStreamerActivityEvents {

    public enum SpotifyStreamerEvents {
        CURR_TRACK_NAME,
        SET_CURR_PLAY_NOW_DATA
    }

    public final SpotifyStreamerEvents event;
    public final String currArtistName;
    public final String currTrackName;
    public final TrackDetails trackDetails;

    private SpotifyStreamerActivityEvents(Builder builder) {
        this.event = builder.event;
        this.currArtistName = builder.currArtistName;
        this.currTrackName = builder.currTrackName;
        this.trackDetails = builder.trackDetails;
    }

    public static class Builder {

        public Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents event) {
            this.event = event;
        }

        private SpotifyStreamerEvents event;
        private String currArtistName;
        private String currTrackName;
        private TrackDetails trackDetails;

        public Builder setCurrArtistName(String currArtistName) {
            this.currArtistName = currArtistName;
            return this;
        }

        public Builder setCurrTrackName(String currTrackName) {
            this.currTrackName = currTrackName;
            return this;
        }

        public Builder setTrackDetails(TrackDetails trackDetails) {
            this.trackDetails = trackDetails;
            return this;
        }

        public SpotifyStreamerActivityEvents build() {
            return new SpotifyStreamerActivityEvents(this);
        }

    }
}