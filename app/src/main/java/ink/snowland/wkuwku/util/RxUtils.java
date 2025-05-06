package ink.snowland.wkuwku.util;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import kotlin.jvm.functions.Function1;

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

    public static <T> Single<T> newSingle(SingleFunction<T> func) {
        return new Single<T>() {

            @Override
            protected void subscribeActual(@io.reactivex.rxjava3.annotations.NonNull SingleObserver<? super T> observer) {
                func.subscribeActual(observer);
            }
        };
    }

    public interface SingleFunction <T> {
        void subscribeActual(SingleObserver<? super T> observer);
    }
}
