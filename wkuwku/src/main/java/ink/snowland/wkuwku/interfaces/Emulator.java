package ink.snowland.wkuwku.interfaces;

import java.net.URL;

public interface Emulator{
    void powerOn();
    void powerOff();
    void reset();
    boolean loadGame(byte[] data);
    void next();
    int getVersion();
}
