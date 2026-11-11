package com.feuerschvenger.perlinsedge.ui.view;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the JavaFX toolkit once for the tests that need a font or a node, and runs code on its thread.
 */
final class FxToolkitSupport {

    private static boolean started;
    private static String unavailableReason;

    private FxToolkitSupport() {}

    /**
     * Starts the toolkit on the first call. A runner with no display cannot start it, so the tests that need it are
     * skipped with the reason, and the failure is remembered so later calls skip at once instead of waiting again.
     */
    private static synchronized void start() {
        if (started) {
            return;
        }
        Assumptions.assumeTrue(unavailableReason == null, () -> String.valueOf(unavailableReason));
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        } catch (RuntimeException | LinkageError noToolkit) {
            unavailableReason = "The JavaFX toolkit cannot start on this machine: " + noToolkit;
            Assumptions.assumeTrue(false, unavailableReason);
        }
        try {
            if (!ready.await(20, TimeUnit.SECONDS)) {
                unavailableReason = "The JavaFX toolkit did not start within 20 seconds";
                Assumptions.assumeTrue(false, unavailableReason);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        Platform.setImplicitExit(false);
        started = true;
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
