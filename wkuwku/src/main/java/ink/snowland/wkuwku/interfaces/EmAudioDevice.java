package ink.snowland.wkuwku.interfaces;

public interface EmAudioDevice extends EmulatorDevice {
    int PCM_16BIT = 1;
    void open(int encoding, int sampleRate, int channels);
    void play(final short[] data, int frames);
    void pause();
    void close();

    boolean isOpen();

    int getSampleRate();
}
