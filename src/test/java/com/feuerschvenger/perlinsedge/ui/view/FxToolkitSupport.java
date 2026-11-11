package com.feuerschvenger.perlinsedge.ui.view;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the JavaFX toolkit once for the tests that need a font or a node, and runs code on its thread.
 */
final class FxToolkitSupport {

    private static boolean started;

    private FxToolkitSupport() {}

    private static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        try {
            if (!ready.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The JavaFX toolkit did not start");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        Platform.setImplicitExit(false);
    }

    /**
     * Runs a task on the JavaFX application thread and returns its result.
     *
     * @param task the task
     * @param <T>  the result type
     * @return what the task returned
     */
    static <T> T onFx(Callable<T> task) {
        start();
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(task.call());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The JavaFX task did not finish");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        if (failure.get() != null) {
            throw new IllegalStateException(failure.get());
        }
        return result.get();
    }
}
