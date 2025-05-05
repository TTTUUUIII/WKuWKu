package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Completable;

public class RxUtils {
    private RxUtils() {}

    public static Completable newCompletable(@NonNull Runnable runnable) {
        return Completable.create(emitter -> {
            try {
                runnable.run();
                emitter.onComplete();
            } catch (Throwable throwable) {
                emitter.onError(throwable);
            }
        });
    }
}
