package com.outlook.wn123o.retrosystem.common;

public class Timing {
    public final double fps;             /* FPS of video content. */
    public final double sampleRate;     /* Sampling rate of audio. */

    public Timing(double fps, double sampleRate) {
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
