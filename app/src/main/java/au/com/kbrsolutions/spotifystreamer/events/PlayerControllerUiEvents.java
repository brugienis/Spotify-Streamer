package au.com.kbrsolutions.spotifystreamer.events;

/**
 * Created by business on 24/07/2015.
 */
public class PlayerControllerUiEvents {

    public final PlayerUiEvents event;
    public final int timeInSecs;

    public enum PlayerUiEvents {START_PLAYING_TRACK, PLAYING_TRACK, PAUSED_TRACK, TRACK_PROGRESS}

    public PlayerControllerUiEvents(PlayerUiEvents event) {
        this(event, -1);
    }

    public PlayerControllerUiEvents(PlayerUiEvents event, int timeInSecs) {
        this.event = event;
        this.timeInSecs = timeInSecs;
    }
}
