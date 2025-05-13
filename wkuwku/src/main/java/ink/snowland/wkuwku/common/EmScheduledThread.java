package ink.snowland.wkuwku.common;

public abstract class EmScheduledThread extends Thread {
    private long mFrameIntervalNS;

    public EmScheduledThread() {
        setPriority(Thread.MAX_PRIORITY);
    }
    public void schedule(double fps) {
        if (!Double.isNaN(fps) && fps != 0) {
            mFrameIntervalNS = (long) Math.floor(1e9 / fps);
        }
        start();
    }

    @Override
    public void run() {
        super.run();
        long prevFrameTimeNS = System.nanoTime();
        while (!isInterrupted()) {
            long currentTimeNS = System.nanoTime();
            if (currentTimeNS - prevFrameTimeNS >= mFrameIntervalNS) {
                next();
                prevFrameTimeNS = 2 * currentTimeNS - System.nanoTime();
            }
        }
    }

    public void setScheduleFps(int fps) {
        mFrameIntervalNS = (long) Math.floor(1e9 / fps);
    }

    public void cancel() {
        interrupt();
    }

    public abstract void next();
}
