package ink.snowland.wkuwku.interfaces;

public interface Emulator{
    boolean load(String path);
    void run();
    void reset();
    void suspend();
}
