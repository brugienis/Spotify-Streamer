package au.com.kbrsolutions.spotifystreamer.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import au.com.kbrsolutions.spotifystreamer.data.TrackDetails;

/**
 * Created by business on 16/07/2015.
 */
public class MusicPlayerService extends Service {

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackDetails> mTracksDetails;
    private int selectedTrack;
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
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind - start");
//        mSomeGuiActivitiesBounded = false;
//        mostRecentUnboundTime = System.currentTimeMillis();
//        if (!mServiceTerminationTaskActive) {
//            new ServiceTerminationTask().execute(Long.valueOf(waitTimeBeforeServiceShutdownAfterLastActivityUnboundSecs) * 1000);
//        }
//		mNoGuiActivitiesBounded = true;
//		stopForegroundIfNoActivityBoundedOverOneMinute();
        Log.i(LOG_TAG, "onUnbind - end");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onCreate - start");
        mMediaPlayer = new MediaPlayer();
        configurePlayer();
        Log.i(LOG_TAG, "onCreate - end");
    }

    private void configurePlayer() {
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
    }

    private void handleOnCompletion() {
        Log.i(LOG_TAG, "handleOnCompletion - start");
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    private void handleOnPrepared(MediaPlayer player) {
        int duration = player.getDuration();
        Log.i(LOG_TAG, "handleOnPrepared - start - duration: " + duration);
        player.start();
    }

    private boolean handleOnError(MediaPlayer mp, int what, int extra) {
        Log.i(LOG_TAG, "handleOnError - start");
        return false;
    }
    public class LocalBinder extends Binder {

        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    /**
     * Passes trackd of the selected artist.
     *
     * @param trackDetails - top 10 tracks of the selected artist
     */
    public void setTracks(ArrayList<TrackDetails> trackDetails) {
        mTracksDetails = trackDetails;
    }

    public void playTrack(TrackDetails trackDetails) {
        if (mMediaPlayer.isPlaying()) {
            Log.i(LOG_TAG, "handleOnError - playTrack - pause requested");
            mMediaPlayer.pause();
            return;
        }
        Log.i(LOG_TAG, "handleOnError - playTrack - previewUrl: " + trackDetails.previewUrl);
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(trackDetails.previewUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public void resume() {
        mMediaPlayer.start();
    }

}
