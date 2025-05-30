package ink.snowland.wkuwku.common;

public class EmSystemTiming {
    public final double fps;             /* FPS of video content. */
    public final double sampleRate;     /* Sampling rate of audio. */

    public EmSystemTiming(double fps, double sampleRate) {
        this.fps = fps;
        this.sampleRate = sampleRate;
    }

    @Override
    public String toString() {
        return "EmSystemTiming{" +
                "fps=" + fps +
                ", sampleRate=" + sampleRate +
                '}';
    }
}
