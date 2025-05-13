package ink.snowland.wkuwku.common;


public abstract class EmScheduledThread extends Thread {
    private long mFrameIntervalNS;

    public EmScheduledThread() {
        setPriority(Thread.MAX_PRIORITY);
    }

    public void schedule(double fps) {
        if (fps > 0) {
            mFrameIntervalNS = (long) Math.floor(1e9 / fps);
        } else {
            mFrameIntervalNS = 0;
        }
        start();
    }
    @Override
    public void run() {
        super.run();
        long start = System.nanoTime();
        while (!isInterrupted()) {
            long current = System.nanoTime();
            if (current - start >= mFrameIntervalNS) {
                next();
                start = current;
            }
        }
    }

    public void cancel() {
        interrupt();
    }

    public abstract void next();
}
