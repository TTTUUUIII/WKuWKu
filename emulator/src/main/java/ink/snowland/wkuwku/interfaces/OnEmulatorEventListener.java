package ink.snowland.wkuwku.interfaces;

import androidx.annotation.NonNull;

import ink.snowland.wkuwku.common.EmMessageExt;

public interface OnEmulatorEventListener {
    void onPixelFormatChanged(int format);
    void onRotationChanged(int rotation);
    void onDrawFramebuffer(final byte[] data, int width, int height, int pitch);
    short onGetInputState(int port, int device, int index, int id);
    boolean onRumbleEvent(int port, int effect, int streng);
    void onMessage(@NonNull EmMessageExt message);
}
