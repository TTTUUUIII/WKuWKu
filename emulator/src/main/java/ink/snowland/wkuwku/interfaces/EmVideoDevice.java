package ink.snowland.wkuwku.interfaces;

public interface EmVideoDevice extends EmulatorDevice{
    void refresh(final byte[] data, int width, int height, int pitch);
    void setPixelFormat(int format);
    void setScreenRotation(int rotation);
}
