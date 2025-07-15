package ink.snowland.wkuwku.interfaces;

import ink.snowland.wkuwku.common.EmMessageExt;

public interface OnEmulatorV2EventListener {
    void onVideoSizeChanged(int vw, int vh);
    int onPollInputState(int port, int device, int index, int id);
    void onMessage(EmMessageExt msg);
}
