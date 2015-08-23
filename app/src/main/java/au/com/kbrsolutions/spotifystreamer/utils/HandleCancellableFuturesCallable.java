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
