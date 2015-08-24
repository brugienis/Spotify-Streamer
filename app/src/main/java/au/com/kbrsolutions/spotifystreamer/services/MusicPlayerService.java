package au.com.kbrsolutions.spotifystreamer.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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
    private String mArtistName;
    private ArrayList<TrackDetails> mTracksDetails;
    private int mSelectedTrack;
    private boolean mIsForegroundStarted;
    private boolean mIsBounded;
    private boolean mIsPlayerActive;
    private boolean isPlayerControllerUiActive;
    private boolean isRegisterForPlayNowEvents;
    protected Handler handler = new Handler();
    int trackPlaydurationMsec = -1;
    private HandleCancellableFuturesCallable handleCancellableFuturesCallable;
    private ExecutorService mExecutorService;
    private StopForegroundRunnable stopForegroundRunnable;
    private long WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS = 60L * 1000;
    private long mostRecentClientDisconnectFromServiceTimeMillis;
    private CountDownLatch callBackResultsCountDownLatch;
    private EventBus eventBus;
    private LocalBinder mLocalBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 2015;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicBoolean isPausing = new AtomicBoolean(false);
    private AtomicInteger connectedClientsCnt = new AtomicInteger(0);
    private static final String PLAY_PREV_TRACK = "play_prev_track";
    private static final String PLAY_TRACK = "play_track";
    private static final String PAUSE_TRACK = "pause_track";
    private static final String PLAY_NEXT_TRACK = "play_next_track";

    private final String LOG_TAG = MusicPlayerService.class.getSimpleName();

    /**
     *
     * Called from the notification - performs required action.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {

                case PLAY_PREV_TRACK:
                    if (mSelectedTrack > 0) {
                        playPrevTrack();
                    }
                    break;

                case PLAY_TRACK:
                    if (isPausing.get()) {
                        resume();
                    }
                    break;

                case PAUSE_TRACK:
                    if (isPlaying.get()) {
                        pause();
                    }
                    break;

                case PLAY_NEXT_TRACK:
                    if (mSelectedTrack < mTracksDetails.size() - 1) {
                        playNextTrack();
                    }
                    break;

                default:
                    // TODO: 14/08/2015 - do not throw exception in release version
                    throw new RuntimeException("MusicPlayerService.onStartCommand - in incorrect action: " + action);

            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();

        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
        return mLocalBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
        return;
    }

    /**
     * client should call this method after they got connected to this service - after they got call to the onServiceConnected(...).
     * OnBind will not be called every time the activity connects to the service - see:
     *      https://groups.google.com/forum/#!msg/android-developers/2IegSgtGxyE/iXP3lBCH5SsJ
     *
     * Always set mIsBounded to true.
     */
    public void processAfterConnectedToService(boolean playerControllerUi) {
        if (mIsForegroundStarted) {
            stopForeground(true);
            mIsForegroundStarted = false;
        }
        connectedClientsCnt.getAndIncrement();
        if (playerControllerUi) {
            isPlayerControllerUiActive = true;
        }
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        mIsBounded = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (eventBus == null) {
            eventBus = EventBus.getDefault();
            eventBus.register(this);
        }
        mExecutorService = Executors.newCachedThreadPool();
        startFuturesHandlers("onCreate");
        configurePlayer();
    }

    private void configurePlayer() {
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

    private boolean isLastTrackPlayed;
    private void handleOnCompletion() {
        isPlaying.set(false);
        isPausing.set(true);
        handleCancellableFuturesCallable.cancelCurrFuture();
        waitForPlayer("playTrack");
        if (mSelectedTrack < mTracksDetails.size() - 1) {
            playNextTrack();
        } else {
            isLastTrackPlayed = true;
            sendMessageToPlayerUi(
                    new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PAUSED_TRACK)
                            .build());
            sendMessageToSpotifyStreamerActivity(new SpotifyStreamerActivityEvents.Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents.PLAYER_STAUS)
                    .setCurrPlayerStatus(getString(R.string.player_stopped))
                    .build());
            sendNotification();
            mIsPlayerActive = false;
            if (connectedClientsCnt.get() == 0) {
                scheduleStopForegroundChecker("handleOnCompletion");
            }
        }
    }

    private void playPrevTrack() {
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
        playTrack(mTracksDetails.get(mSelectedTrack));
    }

    private void handleOnPrepared(MediaPlayer player) {
        trackPlaydurationMsec = player.getDuration();
        isPlaying.set(true);
        player.start();
        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.START_PLAYING_TRACK)
                .setDurationTimeInSecs(trackPlaydurationMsec / 1000)
                .build());
        sendMessageToSpotifyStreamerActivity(new SpotifyStreamerActivityEvents.Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents.PLAYER_STAUS)
                .setCurrPlayerStatus(getString(R.string.player_playing))
                .build());
        sendNotification();
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
    }

    private boolean handleOnError(MediaPlayer mp, int what, int extra) {
        handleCancellableFuturesCallable.cancelCurrFuture();
        configurePlayer();
        return false;
    }

    public class LocalBinder extends Binder {

        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    private TrackDetails currPlayingTrackDetails;

    private void waitForPlayer(String source) {
        try {
            callBackResultsCountDownLatch.await();
        } catch (InterruptedException nothingCanBeDone) {
            Toast.makeText(getApplicationContext().getApplicationContext(),
                    getApplicationContext().getResources()
                            .getString(R.string.search_unsuccessful_internal_problems),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called to start track playing - will start async prepare
     */
    private void playTrack(TrackDetails trackDetails) {
        isPlaying.set(false);
        isPausing.set(false);
        isLastTrackPlayed = false;
        if (connectedClientsCnt.get() > 0 && isRegisterForPlayNowEvents) {
            sendMessageToSpotifyStreamerActivity(new SpotifyStreamerActivityEvents.Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents.CURR_TRACK_INFO)
                    .setCurrArtistName(mArtistName)
                    .setTrackDetails(mTracksDetails.get(mSelectedTrack))
                    .setCurrPlayerStatus(getString(R.string.player_preparing))
                    .build());
        }
        sendNotification();
        if (mMediaPlayer == null) {
            configurePlayer();
        }
        waitForPlayer("playTrack");
        mIsPlayerActive = true;
        currPlayingTrackDetails = trackDetails;
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
            mIsPlayerActive = true;
            sendNotification();
        }
    }

    public void resume() {
        isLastTrackPlayed = false;
        waitForPlayer("resume");
        mIsPlayerActive = true;
        // TODO: 29/07/2015 - something wrong with the if below?
        if (mMediaPlayer == null) {
            if (currPlayingTrackDetails != null) {
                playTrack(currPlayingTrackDetails);
            }
            return;
        }
        mMediaPlayer.start();
        isPlaying.set(true);
        isPausing.set(false);
        handleCancellableFuturesCallable.submitCallable(new TrackPlayProgressCheckerCallable());
        sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PLAYING_TRACK)
                .build());
        sendNotification();
    }

    private String prevAlbumLargeImageUrl;
    private Bitmap prevAlbumLargeImageBitmap;

    private void sendNotification() {
        if (mIsForegroundStarted) {
            TrackDetails selectedTrackDetails = mTracksDetails.get(mSelectedTrack);
            if (prevAlbumLargeImageUrl == selectedTrackDetails.albumArtLargeImageUrl) {
                NotificationManager mNotificationManager =
                        (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIFICATION_ID, buildNotification(prevAlbumLargeImageBitmap));
            } else {
                prevAlbumLargeImageUrl = selectedTrackDetails.albumArtLargeImageUrl;
//                Picasso.with(this).load(currPlayingTrackDetails.albumArtLargeImageUrl).resize(50, 50).into(target);
                Picasso.with(this).load(selectedTrackDetails.albumArtLargeImageUrl).into(target);
            }
        }
    }

    /**
     * from http://stackoverflow.com/questions/20181491/use-picasso-to-get-a-callback-with-a-bitmap
     * check also https://github.com/square/picasso/issues/308
     */
    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            prevAlbumLargeImageBitmap = bitmap;
            NotificationManager mNotificationManager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, buildNotification(bitmap));
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.i(LOG_TAG, "onBitmapFailed - start");
        }
    };

    /**
     * Build Notification that will be sent to the system when app is stopped.
     */
    private Notification buildNotification(Bitmap largeIcon) {
        int prevIcon = R.drawable.ic_action_previous;
        int playIcon = R.drawable.ic_action_play;
        int pauseIcon = R.drawable.ic_action_pause;
        int nextIcon = R.drawable.ic_action_next;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_action_next)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(getString(R.string.service_notification_title));

        if (mSelectedTrack > 0) {
            Intent prevIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            prevIntent.setAction(PLAY_PREV_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent prevPendingIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(prevIcon, "", prevPendingIntent);
        }

        TrackDetails selectedTrackDetails = mTracksDetails.get(mSelectedTrack);
        if (isPausing.get()) {
            Intent playIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            playIntent.setAction(PLAY_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent playPendingIntent = PendingIntent.getService(getApplicationContext(), 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(playIcon, "", playPendingIntent);
            mBuilder.setContentText(getString(R.string.service_notification_pausing_text, selectedTrackDetails.trackName));
            mBuilder.setSmallIcon(R.drawable.ic_pause_circle_outline_black_48dp);
        } else if (isPlaying.get()) {
            Intent pauseIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            pauseIntent.setAction(PAUSE_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(pauseIcon, "", pausePendingIntent);
            mBuilder.setContentText(getString(R.string.service_notification_playing_text, selectedTrackDetails.trackName));
            mBuilder.setSmallIcon(R.drawable.ic_play_circle_outline_black_48dp);
        } else {
            mBuilder.setContentText(getString(R.string.service_notification_preparing_text, selectedTrackDetails.trackName));
        }

        if (mSelectedTrack < mTracksDetails.size() - 1) {
            Intent nextIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            nextIntent.setAction(PLAY_NEXT_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent nextPendingIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(nextIcon, "", nextPendingIntent);
        } else if (isLastTrackPlayed) {
            mBuilder.setContentText(getString(R.string.service_notification_stopped_text, selectedTrackDetails.trackName));
        }

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
    }

    /**
     * Leave for later
     *
     */
    @TargetApi(21)
    private Notification buildNotificationApi21Plus(Bitmap largeIcon) {
        int prevIcon = R.drawable.ic_action_previous;
        int playIcon = R.drawable.ic_action_play;
        int pauseIcon = R.drawable.ic_action_pause;
        int nextIcon = R.drawable.ic_action_next;

        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
//                        .setColor(resources.getColor(R.color.sunshine_light_blue))
                        .setSmallIcon(R.drawable.ic_action_next)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(getString(R.string.service_notification_title));

        if (mSelectedTrack > 0) {
            Intent prevIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            prevIntent.setAction(PLAY_PREV_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent prevPendingIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(prevIcon, "", prevPendingIntent);
        }

        TrackDetails selectedTrackDetails = mTracksDetails.get(mSelectedTrack);
        if (isPausing.get()) {
            Intent playIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            playIntent.setAction(PLAY_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent playPendingIntent = PendingIntent.getService(getApplicationContext(), 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(playIcon, "", playPendingIntent);
            mBuilder.setContentText(getString(R.string.service_notification_pausing_text, selectedTrackDetails.trackName));
        } else if (isPlaying.get()) {
            Intent pauseIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            pauseIntent.setAction(PAUSE_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(pauseIcon, "", pausePendingIntent);
            mBuilder.setContentText(getString(R.string.service_notification_playing_text, selectedTrackDetails.trackName));
        } else {
            mBuilder.setContentText(getString(R.string.service_notification_preparing_text, selectedTrackDetails.trackName));
        }

        if (mSelectedTrack < mTracksDetails.size() - 1) {
            Intent nextIntent = new Intent(getApplicationContext(), MusicPlayerService.class);
            nextIntent.setAction(PLAY_NEXT_TRACK); // PendingIntents "lose" their extras if no action is set.
            PendingIntent nextPendingIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.addAction(nextIcon, "", nextPendingIntent);
        } else {
            mBuilder.setContentText(getString(R.string.service_notification_stopped_text, selectedTrackDetails.trackName));
        }

        /* API 21 - start */
        mBuilder
                .setStyle(new Notification.MediaStyle())
                .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0 /* #1: pause button */));
//                .setMediaSession(mMediaSession.getSessionToken())
//        ;
        /* API 21 - end */

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
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        if (connectedClientsCnt.get() == 0) {
            if (!mIsForegroundStarted) {
                if (mTracksDetails != null) {       /* do not start foreground if no tracks data passed to the service */
                    startForeground(NOTIFICATION_ID, buildNotification(null));
                    mIsForegroundStarted = true;
                }
            }
            scheduleStopForegroundChecker("processBeforeDisconnectingFromService");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    /**
     * Runnable that will start after all clients are disconnected from the service. If player is
     * not active and enough time has elapsed, the service will shutdown.
     */
    private class StopForegroundRunnable implements Runnable {

        StopForegroundRunnable(String source) {
        }

        @Override
        public void run() {
            if (connectedClientsCnt.get() == 0 &&
                    !mIsPlayerActive &&
                    System.currentTimeMillis() - mostRecentClientDisconnectFromServiceTimeMillis > (WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS)) {
                mMediaPlayer.release();
                mMediaPlayer = null;
                stopForeground(true);

                stopSelf();
                mTracksDetails = null;
                mSelectedTrack = 0;
                
                stopFuturesHandlers();
            } else {
                scheduleStopForegroundChecker("StopForegroundRunnable.run");
            }
        }
    }

    private void scheduleStopForegroundChecker(String source) {
        if (stopForegroundRunnable != null) {
            handler.removeCallbacks(stopForegroundRunnable);			// remove just in case if there is already one waiting in a queue
        }
        stopForegroundRunnable = new StopForegroundRunnable(source + ".scheduleStopForegroundChecker");
        handler.postDelayed(stopForegroundRunnable, WAIT_TIME_BEFORE_SERVICE_SHUTDOWN_AFTER_LAST_ACTIVITY_UNBOUND_MSECS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdownNow();
        stopForeground(true);
        Picasso.with(this).cancelRequest(target);
    }

    /**
     * Called from the client to set up tracks details.
     */
    public void setTracksDetails(String artistName, ArrayList<TrackDetails> tracksDetails, int selectedTrack) {
        mArtistName = artistName;
        mTracksDetails = tracksDetails;
        mSelectedTrack = selectedTrack;
    }

//    public boolean areTracksDetailsSet() {
//        return mTracksDetails != null;
//    }

    /**
     * problem - start playing track and click home button. when the music stops playing and a minute passed the service kills itself. Open app from the recents. The PlayerUi starts and tries to get details from the service that just started and has no details of selected tracks. NullPointerException in MusicPlayerService.sendPlayerStateDetails. PlayerUi was showing as dialog.
     */
    private void sendPlayerStateDetails() {
        if (mTracksDetails == null) {
            sendMessageToPlayerUi(new PlayerControllerUiEvents.Builder(PlayerControllerUiEvents.PlayerUiEvents.PROCESS_PLAYER_STATE)
                            .setTracksDetails(mTracksDetails)
                            .build()
            );
        } else {
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
    }

    private void sendPlayNowData() {
        String playerStatus = isPlaying.get() ?
                getString(R.string.player_playing) :
                isPausing.get() ?
                        getResources().getString(R.string.player_paused) : getString(R.string.player_preparing);
        sendMessageToSpotifyStreamerActivity(new SpotifyStreamerActivityEvents.Builder(SpotifyStreamerActivityEvents.SpotifyStreamerEvents.SET_CURR_PLAY_NOW_DATA)
                .setCurrPlayerStatus(playerStatus)
                .setCurrArtistName(mArtistName)
                .setTrackDetails(currPlayingTrackDetails)
                .build());
    }

    private void sendMessageToPlayerUi(PlayerControllerUiEvents event) {
        if (isPlayerControllerUiActive) {
            eventBus.post(event);
        }
    }

    private void sendMessageToSpotifyStreamerActivity(SpotifyStreamerActivityEvents event) {
        if (isRegisterForPlayNowEvents) {
            eventBus.post(event);
        }
    }

    /**
     * Get messages through Event Bus from Green Robot
     */
    public void onEvent(MusicPlayerServiceEvents event) {
        MusicPlayerServiceEvents.MusicServiceEvents requestEvent = event.event;
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

            case GET_PLAY_NOW_DATA:
                isRegisterForPlayNowEvents = true;
                sendPlayNowData();
                break;

            case REGISTER_FOR_PLAY_NOW_EVENTS:
                isRegisterForPlayNowEvents = true;
                break;

            case UNREGISTER_FOR_PLAY_NOW_EVENTS:
                isRegisterForPlayNowEvents = false;
                break;

//            default:
//                throw new RuntimeException("LOC_CAT_TAG - onEvent - no code to handle requestEvent: " + requestEvent);
        }
    }

    private int mPlayProgressPercentage;

    /**
     * When the player is actively playing, this Callable, will show progres data to client.
     */
    private final class TrackPlayProgressCheckerCallable implements Callable<String> {

        @Override
        public String call() {
            int trackPlayPositionMsec;
            try {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
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
