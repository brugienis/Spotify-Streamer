package au.com.kbrsolutions.spotifystreamer.events;

/**
 * Created by business on 24/07/2015.
 */
public class PlayerControllerUiEvents {

    public final PlayerUiEvents event;
//        public final TrackDetails trackDetails;

    public enum PlayerUiEvents {PLAYING_TRACK, PAUSED_TRACK}

    public PlayerControllerUiEvents(PlayerUiEvents event) {
        this.event = event;
//            this(event, null);
    }

//        public MusicPlayerServiceEvents(MusicServiceEvents event, TrackDetails trackDetails) {
//            this.event = event;
//            this.trackDetails = trackDetails;
}
