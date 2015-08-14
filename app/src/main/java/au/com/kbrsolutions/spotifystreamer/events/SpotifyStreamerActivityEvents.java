package au.com.kbrsolutions.spotifystreamer.events;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 13/08/2015.
 */
public class SpotifyStreamerActivityEvents {

    public enum SpotifyStreamerEvents {
        CURR_TRACK_INFO,
        SET_CURR_PLAY_NOW_DATA,
        PLAYER_STAUS
    }

    public final SpotifyStreamerEvents event;
    public final String currArtistName;
    public final String currPlayerStatus;
    public final TrackDetails trackDetails;

    private SpotifyStreamerActivityEvents(Builder builder) {
        this.event = builder.event;
        this.currArtistName = builder.currArtistName;
        this.currPlayerStatus = builder.currPlayerStatus;
        this.trackDetails = builder.trackDetails;
    }

    public static class Builder {

        public Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents event) {
            this.event = event;
        }

        private SpotifyStreamerEvents event;
        private String currArtistName;
        private String currPlayerStatus;
        private TrackDetails trackDetails;

        public Builder setCurrArtistName(String currArtistName) {
            this.currArtistName = currArtistName;
            return this;
        }

        public Builder setCurrPlayerStatus(String currPlayerStatus) {
            this.currPlayerStatus = currPlayerStatus;
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