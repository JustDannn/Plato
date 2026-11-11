package id.plato.core;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Menjalankan panggilan jaringan di luar JavaFX Application Thread. */
public final class Async {
    private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "plato-io");
        thread.setDaemon(true);
        return thread;
    });

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, Dialogs::error);
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            if (error instanceof ApiException api && api.getStatus() == 401 && Session.isActive()) {
                PlatoApp.sessionExpired();
            } else {
                onError.accept(error);
            }
        });
        POOL.execute(task);
    }

    public static void runVoid(ThrowingRunnable work, Runnable onSuccess) {
        run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run());
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
