package com.feuerschvenger.perlinsedge.ui.view;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the JavaFX toolkit once for the tests that need a font or a node, and runs code on its thread.
 * Where the toolkit cannot start, the tests that need it are skipped on a developer machine and fail when the
 * environment variable {@code CI} is set, so a broken virtual display on a build server is noticed instead of
 * hiding every JavaFX test.
 */
public final class FxToolkitSupport {

    private static boolean started;
    private static String unavailableReason;

    private FxToolkitSupport() {}

    /**
     * Reports that the toolkit is not usable: skips the running test, or fails it when the build runs on CI.
     *
     * @param reason why the toolkit is not usable
     * @param ci     value of the {@code CI} environment variable, null or blank when it is not set
     */
    static void reportUnavailable(String reason, String ci) {
        if (ci != null && !ci.isBlank()) {
            throw new AssertionError(reason + " (CI is set, so the JavaFX tests are not skipped)");
        }
        Assumptions.assumeTrue(false, reason);
    }

    /**
     * Starts the toolkit on the first call. A runner with no display cannot start it, and the failure is remembered
     * so later calls report it at once instead of waiting again.
     */
    private static synchronized void start() {
        if (started) {
            return;
        }
        if (unavailableReason != null) {
            reportUnavailable(unavailableReason, System.getenv("CI"));
        }
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        } catch (RuntimeException | LinkageError noToolkit) {
            unavailableReason = "The JavaFX toolkit cannot start on this machine: " + noToolkit;
            reportUnavailable(unavailableReason, System.getenv("CI"));
        }
        try {
            if (!ready.await(20, TimeUnit.SECONDS)) {
                unavailableReason = "The JavaFX toolkit did not start within 20 seconds";
                reportUnavailable(unavailableReason, System.getenv("CI"));
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
    public static <T> T onFx(Callable<T> task) {
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
