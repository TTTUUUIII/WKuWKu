package ink.snowland.wkuwku.common;

import androidx.annotation.Nullable;

public interface Callable<T> {
    void call(@Nullable T t);
}
