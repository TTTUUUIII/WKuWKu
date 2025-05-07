package ink.snowland.wkuwku.common;


public abstract class EmScheduledThread extends Thread {
    private long mFrameIntervalNS;

    public void schedule(double fps) {
        mFrameIntervalNS = (long) Math.floor(1e9 / fps);
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
