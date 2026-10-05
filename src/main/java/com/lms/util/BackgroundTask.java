package com.lms.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs blocking work (typically JDBC reads) off the JavaFX Application Thread.
 * The callbacks are always invoked on the FX thread, so they may touch UI controls directly.
 */
public final class BackgroundTask {

    private static final Logger LOG = Logger.getLogger(BackgroundTask.class.getName());

    // Daemon threads so a query still in flight never keeps the JVM alive after the window closes.
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "lms-background");
        thread.setDaemon(true);
        return thread;
    });

    private BackgroundTask() {}

    /** Must be called from the FX thread, which is also where onSuccess / onError run. */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            LOG.log(Level.WARNING, "Background task failed", task.getException());
            onError.accept(task.getException());
        });
        EXECUTOR.execute(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
