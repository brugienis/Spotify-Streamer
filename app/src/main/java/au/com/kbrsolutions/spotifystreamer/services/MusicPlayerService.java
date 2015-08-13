package au.com.kbrsolutions.spotifystreamer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.activities.SpotifyStreamerActivity;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;
import au.com.kbrsolutions.spotifystreamer.events.MusicPlayerServiceEvents;
import au.com.kbrsolutions.spotifystreamer.events.PlayerControllerUiEvents;
import au.com.kbrsolutions.spotifystreamer.events.SpotifyStreamerActivityEvents;
import au.com.kbrsolutions.spotifystreamer.utils.HandleCancellableFuturesCallable;
import de.greenrobot.event.EventBus;

/**
 * Created by business on 16/07/2015.
 */
public class MusicPlayerService extends Service {

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackDetails> mTracksDetails;
    private int mSelectedTrack;
    private boolean mIsForegroundStarted;
    private boolean mIsBounded;
    private boolean mIsPlayerActive;
    private boolean isPlayerControllerUiActive;
    protected Handler handler = new Handler();
    int trackPlaydurationMsec = -1;
    private HandleCancellableFuturesCallable handleCancellableFuturesCallable;
    private ExecutorService mExecutorService;
    private StopForegroundRunnable stopForegroundRunnable;
    private long WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS = 60L * 1000;
    private long mostRecentClientDisconnectFromServiceTimeMillis;
    //    private int selectedTrack;
    private CountDownLatch callBackResultsCountDownLatch;
    private EventBus eventBus;
    private LocalBinder mLocalBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 2015;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicBoolean isPausing = new AtomicBoolean(false);
    private AtomicInteger connectedClientsCnt = new AtomicInteger(0);

    private final String LOG_TAG = MusicPlayerService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        Log.i(LOG_TAG, "onBind - start");

        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
        return mLocalBinder;
    }

    @Override
    public void onRebind(Intent intent) {
//        Log.i(LOG_TAG, "onRebind - start");
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
        return;
    }

//    public void reconnectedToMusicPlayerService(ArrayList<TrackDetails> tracksDetails, int selectedTrack) {
//        mTracksDetails = tracksDetails;
//        mSelectedTrack = selectedTrack;
//        Log.i(LOG_TAG, "reconnectedToMusicPlayerService - start - mSelectedTrack/mTracksDetails: " + mSelectedTrack + "/" + mTracksDetails);
//        if (stopForegroundRunnable != null) {
//            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
//        }
//        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.START_PLAYING_TRACK)
//                .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
//                .build());
//    }

    /**
     * client should call this method after they got connected to this service - after they got call to the onServiceConnected(...).
     * OnBind will not be called every time the activity connects to the service - see:
     *      https://groups.google.com/forum/#!msg/android-developers/2IegSgtGxyE/iXP3lBCH5SsJ
     *
     * Always set mIsBounded to true.
     */
    public void processAfterConnectedToService(boolean playerControllerUi) {
        connectedClientsCnt.getAndIncrement();
        if (playerControllerUi) {
            isPlayerControllerUiActive = true;
        }
        Log.i(LOG_TAG, "processAfterConnectedToService - start - connectedClientsCnt/isPlayerControllerUiActive: " + connectedClientsCnt.get() + "/" + isPlayerControllerUiActive);
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
//        isClientActive = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.i(LOG_TAG, "onCreate - start");
        if (eventBus == null) {
            eventBus = EventBus.getDefault();
            eventBus.register(this);
            Log.i(LOG_TAG, "onCreate - after eventBus.register");
        }
        mExecutorService = Executors.newCachedThreadPool();
        startFuturesHandlers("onCreate");
        configurePlayer();
//        Log.i(LOG_TAG, "onCreate - end");
    }

    private void configurePlayer() {
//        Log.i(LOG_TAG, "configurePlayer - start");
        callBackResultsCountDownLatch = new CountDownLatch(1);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                handleOnPrepared(mp);
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                handleOnCompletion();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return handleOnError(mp, what, extra);
            }
        });
        callBackResultsCountDownLatch.countDown();
//        Log.i(LOG_TAG, "configurePlayer - end");
    }

    private Future<String> cancellableFuture;

    private void startFuturesHandlers(String source) {
        if (cancellableFuture == null) {
            handleCancellableFuturesCallable = new HandleCancellableFuturesCallable(mExecutorService);
            cancellableFuture = mExecutorService.submit(handleCancellableFuturesCallable);
        }
    }

    private void stopFuturesHandlers() {
        if (cancellableFuture != null) {
            cancellableFuture.cancel(true);
            cancellableFuture = null;
        }
    }

    private void handleOnCompletion() {
        Log.i(LOG_TAG, "handleOnCompletion - start");
        isPlaying.set(false);
        isPausing.set(true);
        handleCancellableFuturesCallable.cancelCurrFuture();
        waitForPlayer("playTrack");
        if (mSelectedTrack < mTracksDetails.size() - 1) {
            Log.i(LOG_TAG, "handleOnCompletion - calling playNext");
            playNextTrack();
        } else {
            Log.i(LOG_TAG, "handleOnCompletion - last track played - mSelectedTrack/tracks cnt: " + mSelectedTrack + "/" + mTracksDetails.size());
            sendMessageToPlayerUi(
                    new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK)
                            .build());
            mIsPlayerActive = false;
            if (connectedClientsCnt.get() == 0) {
                Log.i(LOG_TAG, "handleOnCompletion - all clients disconnected");
                scheduleStopForegroundChecker("handleOnCompletion");
            }
        }
    }

    private void playPrevTrack() {
        Log.i(LOG_TAG, "playPrevTrack - start - mSelectedTrack: " + mSelectedTrack);
        if (mSelectedTrack > 0) {
            handleCancellableFuturesCallable.cancelCurrFuture();
            --mSelectedTrack;
            sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PREPARING_TRACK)
                    .setSselectedTrack(mSelectedTrack)
                    .setIsFirstTrackSelected(mSelectedTrack == 0)
                    .build());
            playTrack(mTracksDetails.get(mSelectedTrack));
        }
    }

    private void playNextTrack() {
        handleCancellableFuturesCallable.cancelCurrFuture();
        ++mSelectedTrack;
        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PREPARING_TRACK)
                .setSselectedTrack(mSelectedTrack)
                .setIsLastTrackSelected(mSelectedTrack == mTracksDetails.size() - 1)
                .build());
        eventBus.post(new SpotifyStreamerActivityEvents.Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents.CURR_TRACK_NAME)
                        .setCurrTrackName(mTracksDetails.get(mSelectedTrack).trackName)
                        .build()
        );
        playTrack(mTracksDetails.get(mSelectedTrack));
    }

    private void handleOnPrepared(MediaPlayer player) {
        trackPlaydurationMsec = player.getDuration();
        // TODO: 30/07/2015 - not sure if that is the best place to startForeground
        if (!mIsForegroundStarted) {
            startForeground(NOTIFICATION_ID, buildNotification());
            mIsForegroundStarted = true;
        }
        isPlaying.set(true);
        player.start();
        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.START_PLAYING_TRACK)
                .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
                .build());
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
    }

    private boolean handleOnError(MediaPlayer mp, int what, int extra) {
        Log.i(LOG_TAG, "handleOnError - start - what/extra: " + what + "/" + extra);
        handleCancellableFuturesCallable.cancelCurrFuture();
        configurePlayer();
        Log.i(LOG_TAG, "handleOnError - start - mMediaPlayer: " + mMediaPlayer);
        return false;
    }
    public class LocalBinder extends Binder {

        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    TrackDetails currTrackDetails;

    // TODO: 29/07/2015 not sure if I need below. Just do not do anything that would caused Error?
    private void waitForPlayer(String source) {
//        Log.i(LOG_TAG, "waitForPlayer - start - source: " + source);
        try {
            callBackResultsCountDownLatch.await();
        } catch (InterruptedException nothingCanBeDone) {
            Toast.makeText(getApplicationContext().getApplicationContext(),
                    getApplicationContext().getResources()
                            .getString(R.string.search_unsuccessful_internal_problems),
                    Toast.LENGTH_LONG).show();
        }
//        Log.i(LOG_TAG, "waitForPlayer - end   - source: " + source);
    }

    public void playTrack(TrackDetails trackDetails) {
//        Log.i(LOG_TAG, "playTrack - previewUrl: " + trackDetails.previewUrl);
        isPlaying.set(false);
        isPausing.set(false);
        if (mMediaPlayer == null) {
            configurePlayer();
        }
        waitForPlayer("playTrack");
        mIsPlayerActive = true;
        currTrackDetails = trackDetails;
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(trackDetails.previewUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        waitForPlayer("pause");
        if (mMediaPlayer != null) {
            isPlaying.set(false);
            isPausing.set(true);
            mMediaPlayer.pause();
            handleCancellableFuturesCallable.cancelCurrFuture();
            sendMessageToPlayerUi(
                    new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK)
                            .build());
            mIsPlayerActive = false;
        }
    }

    public void resume() {
        waitForPlayer("resume");
        mIsPlayerActive = true;
        // TODO: 29/07/2015 - something wrong with the if below?
        if (mMediaPlayer == null) {
            if (currTrackDetails != null) {
                playTrack(currTrackDetails);
            }
            return;
        }
        mMediaPlayer.start();
        isPlaying.set(true);
        isPausing.set(false);
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PLAYING_TRACK)
                .build());
    }

    private Notification buildNotification() {
//        Notification notification;
//        Notification.Builder builder = new Notification.Builder(getApplicationContext());
//        builder.setSmallIcon(R.drawable.ic_action_next);
//        builder.setContentTitle(getString(R.string.service_notification_title));
//        builder.setContentText(getString(R.string.service_notification_text));
//        notification = builder.build();
////		notification.flags = notification.flags | notification.FLAG_FOREGROUND_SERVICE; not needed
//        //		Log.i(LOC_CAT_TAG, "buildNotification - end - notification: " + notification);
//        return notification;


        // NotificationCompatBuilder is a very convenient way to build backward-compatible
        // notifications.  Just throw in some data.
        String text = getString(R.string.service_notification_text);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
//                        .setColor(resources.getColor(R.color.sunshine_light_blue))
                        .setSmallIcon(R.drawable.ic_action_next)
//                        .setLargeIcon(R.drawable.ic_action_next)
                        .setContentTitle(getString(R.string.service_notification_title))
                                .setContentText(text);
        // Make something interesting happen when the user clicks on the notification.
        // In this case, opening the app is sufficient.
        Intent resultIntent = new Intent(getApplicationContext(), SpotifyStreamerActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();

//        NotificationManager mNotificationManager =
//                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
//        mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * client should call this method after they got connected to this service - after they got call to the onServiceConnected(...).
     * OnBind will not be called every time the activity connects to the service - see:
     *      https://groups.google.com/forum/#!msg/android-developers/2IegSgtGxyE/iXP3lBCH5SsJ
     *
     * Always set mIsBounded to true.
     */
    public void processBeforeDisconnectingFromService(boolean playerControllerUi) {
        mostRecentClientDisconnectFromServiceTimeMillis = System.currentTimeMillis();
        connectedClientsCnt.getAndDecrement();
        if (playerControllerUi) {
            isPlayerControllerUiActive = false;
        }
        Log.i(LOG_TAG, "processBeforeDisconnectingFromService - start - connectedClientsCnt/isPlayerControllerUiActive: " + connectedClientsCnt.get() + "/" + isPlayerControllerUiActive);
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        if (connectedClientsCnt.get() == 0) {
            Log.i(LOG_TAG, "processBeforeDisconnectingFromService - no connected clients and player is not active");
//            isClientActive = false;
            scheduleStopForegroundChecker("processBeforeDisconnectingFromService");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind - start");
//        mIsBounded = false;
//        if (!mIsPlayerActive) {
//            Log.i(LOG_TAG, "onUnbind - player is not active");
//            scheduleStopForegroundChecker("onUnbind");
//        }
        Log.i(LOG_TAG, "onUnbind - end");
        return false;
    }

    private class StopForegroundRunnable implements Runnable {

        StopForegroundRunnable(String source) {
//            Log.i(LOG_TAG, "StopForegroundRunnable.constructor - source: " + source);
        }

        @Override
        public void run() {
//            Log.i(LOG_TAG, "StopForegroundRunnable.run - start");
            if (connectedClientsCnt.get() == 0 &&
                    !mIsPlayerActive &&
                    System.currentTimeMillis() - mostRecentClientDisconnectFromServiceTimeMillis > (WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS)) {
                Log.i(LOG_TAG, "StopForegroundRunnable.run - calling stopForeground() and stopSelf()");
                mMediaPlayer.release();
                mMediaPlayer = null;
                stopForeground(true);

                // TODO: 8/08/2015 - check if below will stop the service 
                stopSelf();
                
                stopFuturesHandlers();
//                Log.i(LOG_TAG, "StopForegroundRunnable.run - called  stopForeground()");
            } else {
//                Log.i(LOG_TAG, "StopForegroundRunnable.run - scheduling StopForegroindRunnable task again");
                scheduleStopForegroundChecker("StopForegroundRunnable.run");
            }
            Log.i(LOG_TAG, "StopForegroundRunnable.run - end");
        }
    }

    private void scheduleStopForegroundChecker(String source) {
        Log.i(LOG_TAG, "scheduleStopForegroundChecker - source: " + source);
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        stopForegroundRunnable = new StopForegroundRunnable(source + ".scheduleStopForegroundChecker");
        handler.postDelayed(stopForegroundRunnable, WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy - start");
        super.onDestroy();
        mExecutorService.shutdown();
        stopForeground(true);
        Log.i(LOG_TAG, "onDestroy - end");
    }

    public void setTracksDetails(ArrayList<TrackDetails> tracksDetails, int selectedTrack) {
        mTracksDetails = tracksDetails;
        mSelectedTrack = selectedTrack;
//        Log.i(LOG_TAG, "setTracksDetails - start - got mSelectedTrack/track name: " + mSelectedTrack + " - " + tracksDetails.get(mSelectedTrack).trackName);
    }

    private void sendPlayerStateDetails() {
//        eventBus.post(
          sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PROCESS_PLAYER_STATE)
                        .setTracksDetails(mTracksDetails)
                        .setSselectedTrack(mSelectedTrack)
                        .setIsTrackPlaying(isPlaying.get())
                        .setIsTrackPausing(isPausing.get())
                        .setIsFirstTrackSelected(mSelectedTrack == 0)
                        .setIsLastTrackSelected(mSelectedTrack == mTracksDetails.size() - 1)
                        .setPlayProgressPercentage(mPlayProgressPercentage)
                        .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
                        .build()
        );
    }

//    private boolean isClientActive;
    private void sendMessageToPlayerUi(PlayerControllerUiEvents event) {
//        if (isClientActive) {
        if (isPlayerControllerUiActive) {
            eventBus.post(event);
        }
    }

    public void onEvent(MusicPlayerServiceEvents event) {
        MusicPlayerServiceEvents.MusicServiceEvents requestEvent = event.event;
        Log.i(LOG_TAG, "onEvent - start - got event/mSelectedTrack: " + requestEvent + "/" + mSelectedTrack + " - " + Thread.currentThread().getName());
        switch (requestEvent) {

            case PLAY_TRACK:
                playTrack(mTracksDetails.get(mSelectedTrack));
                break;

            case PAUSE_TRACK:
                pause();
                break;

            case RESUME_TRACK:
                resume();
				break;

            case PLAY_PREV_TRACK:
                playPrevTrack();
                break;

            case PLAY_NEXT_TRACK:
                playNextTrack();
                break;

            case GET_PLAYER_STATE_DETAILS:
                sendPlayerStateDetails();
                break;

            default:
                throw new RuntimeException("LOC_CAT_TAG - onEvent - no code to handle requestEvent: " + requestEvent);
        }
    }

    private int mPlayProgressPercentage;

    private final class TrackPlayProgressCheckerCallable implements Callable<String> {

        @Override
        public String call() {//throws Exception {
            int trackPlayPositionMsec;
            boolean wasInterrupted = false;
            try {
                while (true) {
                    // TODO: 28/07/2015 keep internal constant in preferences
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
//                        Log.i(LOG_TAG, "call - INTERRUPTED");
                        wasInterrupted = true;
                        break;
                    }
                    if (!isPlaying.get()) {
                        break;
                    }
                    trackPlayPositionMsec = mMediaPlayer.getCurrentPosition();
                    {
                        mPlayProgressPercentage = trackPlayPositionMsec * 100 / trackPlaydurationMsec;
                        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.TRACK_PLAY_PROGRESS)
                                .setPlayProgressPercentage(mPlayProgressPercentage)
                                .build());
                    }
                }
            } finally {
//                Log.i(LOG_TAG, "called - finally");
//                if (wasInterrupted) {
                if (!isPausing.get()) {
                    sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.TRACK_PLAY_PROGRESS)
                            .setPlayProgressPercentage(0)
                            .build());
                }
            }
            return null;
        }
    }

}
