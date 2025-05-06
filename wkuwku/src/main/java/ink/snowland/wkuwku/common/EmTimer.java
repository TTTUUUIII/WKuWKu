package ink.snowland.wkuwku.common;

import android.view.Choreographer;

import androidx.annotation.NonNull;

public class EmTimer {
    private Runnable mTask;
    private boolean mCanceled = false;
    private long mFrameIntervalNS;

    public void schedule(@NonNull Runnable r, double fps) {
        mTask = r;
        mCanceled = false;
        mFrameIntervalNS = (long) Math.round(1000000000 / fps);
        Choreographer.getInstance().postFrameCallback(this::doWork);
    }

    private void doWork(long frameTimeNanos) {
        mTask.run();
        if (!mCanceled) {
            Choreographer.getInstance().postFrameCallback(this::doWork);
        }
    }

    public void cancel() {
        mCanceled = true;
    }
}
