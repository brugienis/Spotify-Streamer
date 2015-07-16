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
//        implements
//        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
//        MediaPlayer.OnCompletionListener {

    private MediaPlayer player;
    private ArrayList<TrackDetails> tracksDetails;
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
        player = new MediaPlayer();
        configurePlayer();
        Log.i(LOG_TAG, "onCreate - end");
    }

    public void configurePlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                handleOnPrepared();
            }
        });
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                handleOnCompletion();
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return handleOnError();
            }
        });
    }

    private void handleOnCompletion() {
        Log.i(LOG_TAG, "handleOnCompletion - start");
    }

    private void handleOnPrepared() {
        Log.i(LOG_TAG, "handleOnPrepared - start");
        player.start();
    }

    private boolean handleOnError() {
        Log.i(LOG_TAG, "handleOnError - start");
        return false;
    }
    public class LocalBinder extends Binder {

        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    public void playTrack(TrackDetails trackDetails) {
        player.reset();
        try {
            player.setDataSource(trackDetails.previewUrl);
            player.prepareAsync();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Uri trackUri = ContentUris.withAppendedId(
//                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                trackDetails.previewUrl);
    }

//    @Override
//    public void onCompletion(MediaPlayer mp) {
//
//    }
//
//    @Override
//    public boolean onError(MediaPlayer mp, int what, int extra) {
//        return false;
//    }
//
//    @Override
//    public void onPrepared(MediaPlayer mp) {
//
//    }
}
