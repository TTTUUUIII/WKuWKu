package ink.snowland.wkuwku.common;

public class MacroEvent {
    public final int[] keys;
    public final int delayed;
    public final int duration;

    public MacroEvent(int[] keys, int delayed, int duration) {
        this.keys = keys;
        this.delayed = delayed;
        this.duration = duration;
    }
}
