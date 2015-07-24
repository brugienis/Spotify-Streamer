package au.com.kbrsolutions.spotifystreamer.events;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 24/07/2015.
 */
public class MusicPlayerServiceEvents {

    public final MusicServiceEvents event;
    public final TrackDetails trackDetails;

    public enum MusicServiceEvents {PLAY_TRACK, PAUSE_TRACK, RESUME_TRACK}

    public MusicPlayerServiceEvents(MusicServiceEvents event) {
        this(event, null);
    }

    public MusicPlayerServiceEvents(MusicServiceEvents event, TrackDetails trackDetails) {
        this.event = event;
        this.trackDetails = trackDetails;
    }
}
