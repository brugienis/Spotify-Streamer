package au.com.kbrsolutions.spotifystreamer.utils;

import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by business on 28/07/2015.
 */
public class HandleCancellableFuturesCallable implements Callable<String> {

    private CompletionService<String> completionService;
    private Future<String> cancellableFuture;
    private Future<String> currExecutingFuture;
    private boolean stopProcessing = false;

    private final static String LOG_TAG = HandleCancellableFuturesCallable.class.getSimpleName();

    public HandleCancellableFuturesCallable(ExecutorService mExecutorService) {
        completionService = new ExecutorCompletionService<>(mExecutorService);
        Log.v(LOG_TAG, "constructor - completionService: " + completionService);
//        cancellableExecutingTaksCnt.set(0);
    }

    public void submitCallable(Callable<String> callable) {
        cancelCurrFuture();
        currExecutingFuture = completionService.submit(callable);
    }
    /*

08-24 09:35:46.818    3564-3564/au.com.kbrsolutions.spotifystreamer W/dalvikvm﹕ threadid=1: thread exiting with uncaught exception (group=0xa4ba3648)
08-24 09:35:46.822    3564-3564/au.com.kbrsolutions.spotifystreamer E/AndroidRuntime﹕ FATAL EXCEPTION: main
    java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.ExecutorCompletionService$QueueingFuture@52d7a040 rejected from java.util.concurrent.ThreadPoolExecutor@52a50fb0[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 3]
            at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:1979)
            at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:786)
            at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1307)
            at java.util.concurrent.ExecutorCompletionService.submit(ExecutorCompletionService.java:152)
            at au.com.kbrsolutions.spotifystreamer.utils.HandleCancellableFuturesCallable.submitCallable(HandleCancellableFuturesCallable.java:32)
            at au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService.handleOnPrepared(MusicPlayerService.java:314)
            at au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService.access$000(MusicPlayerService.java:49)
            at au.com.kbrsolutions.spotifystreamer.services.MusicPlayerService$1.onPrepared(MusicPlayerService.java:215)

     */

    public void cancelCurrFuture() {
        if (currExecutingFuture != null) {
            if (currExecutingFuture.cancel(true)) {
//                Log.v(LOG_TAG, "cancelCurrFuture - future cancelled");
//                cancellableExecutingTaksCnt.addAndGet(-1);
            }
        }
    }

    @Override
    public String call() {
        try {
            while (!stopProcessing) {
                cancellableFuture = completionService.take();
                // todo: should add finally to cancel? see book
                try {
                    cancellableFuture.get();
                } catch (ExecutionException e) {
                    Log.v(LOG_TAG, "call - exception: " + e);
//                    eventBus.post(new ActivitiesEvents.Builder(HomeEvents.SHOW_MESSAGE)
//                            .setMsgContents("Problems with access to Google Drive - try again")
//                            .build());
                }
//                cancellableExecutingTaksCnt.addAndGet(-1);
            }
        } catch (InterruptedException e) {
            cancelCurrFuture();
            stopProcessing = true;
            // TODO Auto-generated catch block
//				e.printStackTrace();
        }
        return null;
    }
}
