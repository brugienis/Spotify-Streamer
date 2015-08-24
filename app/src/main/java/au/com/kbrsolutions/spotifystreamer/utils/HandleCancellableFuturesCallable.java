package au.com.kbrsolutions.spotifystreamer.utils;

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
    }

    /**
     * cancel current future and submit callable
     */
    public void submitCallable(Callable<String> callable) {
        cancelCurrFuture();
        currExecutingFuture = completionService.submit(callable);
    }

    /**
     * cancel current future
     */
    public void cancelCurrFuture() {
        if (currExecutingFuture != null) {
            if (currExecutingFuture.cancel(true)) {
//                Log.v(LOG_TAG, "cancelCurrFuture - future cancelled");
            }
        }
    }

    @Override
    public String call() {
        try {
            while (!stopProcessing) {
                cancellableFuture = completionService.take();
                try {
                    cancellableFuture.get();
                } catch (ExecutionException nothingCanBeDone) {
                }
            }
        } catch (InterruptedException e) {
            cancelCurrFuture();
            stopProcessing = true;
        }
        return null;
    }
}
