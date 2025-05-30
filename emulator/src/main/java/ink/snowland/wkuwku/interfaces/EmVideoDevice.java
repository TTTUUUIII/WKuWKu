package ink.snowland.wkuwku.interfaces;

public interface EmVideoDevice extends EmulatorDevice{
    int PIXEL_FORMAT_RGBA = 1;
    int PIXEL_FORMAT_RGB565 = 2;
    void refresh(final byte[] data, int width, int height, int pitch);
    void setPixelFormat(int format);
}
