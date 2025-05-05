package ink.snowland.wkuwku.common;

public abstract class EmThread extends Thread {
    {
        setPriority(Thread.MAX_PRIORITY);
    }
    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            next();
        }
    }

    protected abstract void next();
}
