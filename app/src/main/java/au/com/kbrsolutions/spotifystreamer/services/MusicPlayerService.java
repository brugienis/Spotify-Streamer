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
import java.util.concurrent.CountDownLatch;

import au.com.kbrsolutions.spotifystreamer.R;
import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 16/07/2015.
 */
public class MusicPlayerService extends Service {

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackDetails> mTracksDetails;
    private boolean mIsForegroundStarted;
    private static final int NOTIFICATION_ID = 2015;
    protected Handler handler = new Handler();
    private StopForegroundRunnable stopForegroundRunnable;
    private long WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS = 60;
    private long mostRecentUnboundTime;
    //    private int selectedTrack;
    private CountDownLatch callBackResultsCountDownLatch;
    private LocalBinder mLocalBinder = new LocalBinder();

    private final String LOG_TAG = MusicPlayerService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind - start");
        return mLocalBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(LOG_TAG, "onRebind - start");
//        mSomeGuiActivitiesBounded = true;
        return;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onCreate - start");
        configurePlayer();
        Log.i(LOG_TAG, "onCreate - end");
    }

    private void configurePlayer() {
        Log.i(LOG_TAG, "configurePlayer - start");
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
        Log.i(LOG_TAG, "configurePlayer - end");
    }

    private void handleOnCompletion() {
        Log.i(LOG_TAG, "handleOnCompletion - start");
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    private void handleOnPrepared(MediaPlayer player) {
        int duration = player.getDuration();
        if (!mIsForegroundStarted) {
            startForeground(NOTIFICATION_ID, buildNotification());
            mIsForegroundStarted = true;
            Log.i(LOG_TAG, "handleOnPrepared - startForeground executed");
        } else {
            Log.i(LOG_TAG, "handleOnPrepared - startForeground ALREADY active - not executed now");
        }
        Log.i(LOG_TAG, "handleOnPrepared - start - duration: " + duration);
        player.start();
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

    /**
     * Passes tracked of the selected artist.
     *
     * @param trackDetails - top 10 tracks of the selected artist
     */
    public void setTracks(ArrayList<TrackDetails> trackDetails) {
        mTracksDetails = trackDetails;
    }

    TrackDetails currTrackDetails;

    private void waitForPlayer(String source) {
        Log.i(LOG_TAG, "waitForPlayer - start - source: " + source);
        try {
            callBackResultsCountDownLatch.await();
        } catch (InterruptedException nothingCanBeDone) {
            Toast.makeText(getApplicationContext().getApplicationContext(),
                    getApplicationContext().getResources()
                            .getString(R.string.search_unsuccessful_internal_problems),
                    Toast.LENGTH_LONG).show();
        }
        Log.i(LOG_TAG, "waitForPlayer - end   - source: " + source);
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
            mMediaPlayer.pause();
        }
    }

    public void resume() {
        waitForPlayer("resume");
        if (mMediaPlayer == null) {
            if (currTrackDetails != null) {
                playTrack(currTrackDetails);
            }
            return;
        }
        mMediaPlayer.start();
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
        Log.i(LOG_TAG, "onUnbind - start");
        mostRecentUnboundTime = System.currentTimeMillis();
        scheduleStopForegroundChecker();
        Log.i(LOG_TAG, "onUnbind - end");
        return true;
    }

    private class StopForegroundRunnable implements Runnable {

        @Override
        public void run() {
            Log.i(LOG_TAG, "StopForegroundRunnable.run - start");
            if (System.currentTimeMillis() - mostRecentUnboundTime > (WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS * 1000)) {
                Log.i(LOG_TAG, "StopForegroundRunnable.run - calling stopForeground()");
                mMediaPlayer.release();
                mMediaPlayer = null;
                stopForeground(true);
                Log.i(LOG_TAG, "StopForegroundRunnable.run - called  stopForeground()");
            } else {
                Log.i(LOG_TAG, "StopForegroundRunnable.run - scheduling StopForegroindRunnable task again");
                scheduleStopForegroundChecker();
            }
            Log.i(LOG_TAG, "StopForegroundRunnable.run - end");
        }

    }

    private void scheduleStopForegroundChecker() {
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        stopForegroundRunnable = new StopForegroundRunnable();
        handler.postDelayed(stopForegroundRunnable, WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_SECS * 1000);
    })

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy - start");
        super.onDestroy();
//        mExecutorService.shutdown();
        stopForeground(true);
//		eventBus.post(new ActivitiesEvents(ActivitiesEvents.HomeEvents.GOOGLE_DRIVE_SERVICE_STOPPED));
        Log.i(LOG_TAG, "onDestroy - end");
    }

}
