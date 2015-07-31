package au.com.kbrsolutions.spotifystreamer.events;

import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 24/07/2015.
 */
public class MusicPlayerServiceEvents {

    public final MusicServiceEvents event;
    public final ArrayList<TrackDetails> tracksDetails;
    public final int selectedTrack;

    public enum MusicServiceEvents {
//        SET_TRACKS_DETAILS,
        PLAY_TRACK,
        PAUSE_TRACK,
        RESUME_TRACK,
        PLAY_PREV_TRACK,
        PLAY_NEXT_TRACK
    }

    public MusicPlayerServiceEvents(Builder builder) {
        this.event = builder.event;
        this.tracksDetails = builder.tracksDetails;
        this.selectedTrack = builder.selectedTrack;
    }

    public static class Builder {

        public Builder(MusicPlayerServiceEvents.MusicServiceEvents event) {
            this.event = event;
        }

        private MusicServiceEvents event;
        private ArrayList<TrackDetails> tracksDetails;
        private int selectedTrack;

        public Builder setTracksDetails(ArrayList<TrackDetails> tracksDetails) {
            this.tracksDetails = tracksDetails;
            return this;
        }

        public Builder setSelectedTrack(int selectedTrack) {
            this.selectedTrack = selectedTrack;
            return this;
        }

        public MusicPlayerServiceEvents build() {
            return new MusicPlayerServiceEvents(this);
        }

    }
}
