package au.com.kbrsolutions.spotifystreamer.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
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

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;
import au.com.kbrsolutions.spotifystreamer.events.HandleCancellableFuturesCallable;
import au.com.kbrsolutions.spotifystreamer.events.MusicPlayerServiceEvents;
import au.com.kbrsolutions.spotifystreamer.events.PlayerControllerUiEvents;
import de.greenrobot.event.EventBus;

/**
 * Created by business on 16/07/2015.
 */
public class MusicPlayerService extends Service {

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackDetails> mTracksDetails;
    private int mSelectedTrack;
    private boolean mIsForegroundStarted;
    protected Handler handler = new Handler();
    int trackPlaydurationMsec = -1;
    private HandleCancellableFuturesCallable handleCancellableFuturesCallable;
    private ExecutorService mExecutorService;
    private StopForegroundRunnable stopForegroundRunnable;
    private long WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS = 60;
    private long mostRecentUnboundTime;
    //    private int selectedTrack;
    private CountDownLatch callBackResultsCountDownLatch;
    private EventBus eventBus;
    private LocalBinder mLocalBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 2015;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private final String LOG_TAG = MusicPlayerService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        Log.i(LOG_TAG, "onBind - start");
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        return mLocalBinder;
    }

    @Override
    public void onRebind(Intent intent) {
//        Log.i(LOG_TAG, "onRebind - start");
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        return;
    }

    public void reconnectedToMusicPlayerService() {
        Log.i(LOG_TAG, "reconnectedToMusicPlayerService - start");
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.START_PLAYING_TRACK)
                .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
                .build());
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
        isPlaying.set(false);
        handleCancellableFuturesCallable.cancelCurrFuture();
        waitForPlayer("playTrack");
        if (mSelectedTrack < mTracksDetails.size() - 1) {
            ++mSelectedTrack;
            eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PREPARING_NEXT_TRACK)
                    .seSselectedTrack(mSelectedTrack)
                    .build());
            playTrack( mTracksDetails.get(mSelectedTrack));
//            TrackDetails trackDetails = mTracksDetails.get(mSelectedTrack);
//            currTrackDetails = mTracksDetails.get(mSelectedTrack);
//            try {
//                mMediaPlayer.reset();
//                mMediaPlayer.setDataSource(trackDetails.previewUrl);
//                mMediaPlayer.prepareAsync();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else {
            eventBus.post(
                    new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK)
                            .build());
            scheduleStopForegroundChecker("handleOnCompletion");
        }
    }

    private void handleOnPrepared(MediaPlayer player) {
        trackPlaydurationMsec = player.getDuration();
        if (!mIsForegroundStarted) {
            startForeground(NOTIFICATION_ID, buildNotification());
            mIsForegroundStarted = true;
        }
        isPlaying.set(true);
        player.start();
        eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.START_PLAYING_TRACK)
                .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
                .build());
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
    }

    private boolean handleOnError(MediaPlayer mp, int what, int extra) {
        Log.i(LOG_TAG, "handleOnError - start - what/extra: " + what + "/" + extra);
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

    // TODO: 29/07/2015 not sure if I need below. Just do do anything that would caused Error?
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
        Log.i(LOG_TAG, "playTrack - previewUrl: " + trackDetails.previewUrl);
        waitForPlayer("playTrack");
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
            handleCancellableFuturesCallable.cancelCurrFuture();
            mMediaPlayer.pause();
            eventBus.post(
                    new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK)
                            .build());
//            eventBus.post(new PlayerControllerUiEvents(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK));
        }
    }

    public void resume() {
        waitForPlayer("resume");
        // TODO: 29/07/2015 - something wrong with the if below?
        if (mMediaPlayer == null) {
            if (currTrackDetails != null) {
                playTrack(currTrackDetails);
            }
            return;
        }
        mMediaPlayer.start();
        isPlaying.set(true);
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
        eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PLAYING_TRACK)
                .build());
//        eventBus.post(new PlayerControllerUiEvents(PlayerControllerUiEvents.PlayerUiEvents.PLAYING_TRACK));
    }

    private Notification buildNotification() {
        Notification notification;
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_action_next);
        builder.setContentTitle(getString(R.string.service_notification_title));
        builder.setContentText(getString(R.string.service_notification_text));
        notification = builder.build();
//		notification.flags = notification.flags | notification.FLAG_FOREGROUND_SERVICE; not needed
        //		Log.i(LOC_CAT_TAG, "buildNotification - end - notification: " + notification);
        return notification;
    }

    @Override
    public boolean onUnbind(Intent intent) {
//        Log.i(LOG_TAG, "onUnbind - start");
        mostRecentUnboundTime = System.currentTimeMillis();
        scheduleStopForegroundChecker("onUnbind");
//        resultReceiver = null;
        Log.i(LOG_TAG, "onUnbind - end");
        return true;
    }

    private class StopForegroundRunnable implements Runnable {

        StopForegroundRunnable(String source) {
//            Log.i(LOG_TAG, "StopForegroundRunnable.constructor - source: " + source);
        }

        @Override
        public void run() {
//            Log.i(LOG_TAG, "StopForegroundRunnable.run - start");
            if (System.currentTimeMillis() - mostRecentUnboundTime > (WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS * 1000)) {
                Log.i(LOG_TAG, "StopForegroundRunnable.run - calling stopForeground()");
                mMediaPlayer.release();
                mMediaPlayer = null;
                stopForeground(true);
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
//        Log.i(LOG_TAG, "scheduleStopForegroundChecker - source: " + source);
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        stopForegroundRunnable = new StopForegroundRunnable(source + ".scheduleStopForegroundChecker");
        handler.postDelayed(stopForegroundRunnable, WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS * 1000);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy - start");
        super.onDestroy();
//        mExecutorService.shutdown();
        stopForeground(true);
//		eventBus.post(new ActivitiesEvents(ActivitiesEvents.HomeEvents.GOOGLE_DRIVE_SERVICE_STOPPED));
        Log.i(LOG_TAG, "onDestroy - end");
    }

    public void onEvent(MusicPlayerServiceEvents event) {
        MusicPlayerServiceEvents.MusicServiceEvents requestEvent = event.event;
        Log.i(LOG_TAG, "onEvent - start - got event: " + requestEvent + " - " + Thread.currentThread().getName());
        switch (requestEvent) {

            case PLAY_TRACK:
                mTracksDetails = event.tracksDetails;
                mSelectedTrack = event.selectedTrack;
                playTrack(mTracksDetails.get(mSelectedTrack));
                break;

            case PAUSE_TRACK:
                pause();
                break;

            case RESUME_TRACK:
                resume();
				break;

            case PLAY_PREV_TRACK:
                break;

            case PLAY_NEXT_TRACK:
                break;

            default:
                throw new RuntimeException("LOC_CAT_TAG - onEvent - no code to handle requestEvent: " + requestEvent);
        }
    }

    private final class TrackPlayProgressCheckerCallable implements Callable<String> {

        @Override
        public String call() throws Exception {
            int trackPlayPositionMsec;
            try {
                while (true) {
                    // TODO: 28/07/2015 keep internal constant in preferences
                    Thread.sleep(1000);
                    if (!isPlaying.get()) {
                        break;
                    }
                    trackPlayPositionMsec = mMediaPlayer.getCurrentPosition();
                    eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.TRACK_PLAY_PROGRESS)
                            .setPlayProgressPercentage(trackPlayPositionMsec * 100 / trackPlaydurationMsec)
                            .build());
                }
            } finally {
                Log.i(LOG_TAG, "call - finally");
                eventBus.post(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.TRACK_PLAY_PROGRESS)
                        .setPlayProgressPercentage(0)
                        .build());
            }
            return null;
        }
    }

}
